package softuniGallery.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import softuniGallery.bindingModel.ArticleBindingModel;
import softuniGallery.entity.Article;
import softuniGallery.entity.Category;
import softuniGallery.entity.Tag;
import softuniGallery.entity.User;
import softuniGallery.repository.ArticleRepository;
import softuniGallery.repository.CategoryRepository;
import softuniGallery.repository.TagRepository;
import softuniGallery.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.springframework.security.config.http.MatcherType.regex;

@Controller
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TagRepository tagRepository;

    private List<Article> found = new LinkedList<>();

    @GetMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String create(Model model) {
        model.addAttribute("view", "article/create");
        List<Category> categories = this.categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "base-layout";
    }

    @PostMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String createProcess(ArticleBindingModel articleBindingModel, HttpServletRequest servletRequest) {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());
        Category category = this.categoryRepository.findById(articleBindingModel.getCategoryId()).get();
        HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());
        Article articleEntity = new Article(
                articleBindingModel.getTitle(),
                articleBindingModel.getContent(),
                userEntity,
                category,
                tags
        );


        MultipartFile file = articleBindingModel.getPicture();

        uploadFile(articleEntity, file);

        this.articleRepository.saveAndFlush(articleEntity);

        return "redirect:/";
    }

    private void uploadFile(Article articleEntity, MultipartFile file) {
        if (file != null) {
            String originalName = file.getOriginalFilename();
            File imageFile = new File("C:\\Users\\George-Lenovo\\Desktop\\TeamProjectGallery\\gallery\\src\\main\\resources\\static\\images", originalName);
            try {
                file.transferTo(imageFile);
                articleEntity.setImagePath("/images/" + originalName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/article/viewArticles")
    public String listArticles(Model model){
        List<Article> articles = this.articleRepository.findAll();

        model.addAttribute("articles", articles);
        model.addAttribute("view", "article/index");

        return "base-layout";
    }

    @GetMapping("/article/{id}")
    public String details(Model model, @PathVariable Integer id) {

        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        if (!(SecurityContextHolder.getContext().getAuthentication()
                instanceof AnonymousAuthenticationToken)) {
            UserDetails principal = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User entityUser = this.userRepository.findByEmail(principal.getUsername());

            model.addAttribute("user", entityUser);
        }

        Article article = this.articleRepository.findById(id).get();

        model.addAttribute("article", article);
        model.addAttribute("view", "article/details");

        return "base-layout";
    }

    @GetMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable Integer id, Model model) {

        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        List<Category> categories = this.categoryRepository.findAll();
        model.addAttribute("categories", categories);

        String tagString = article.getTags().stream().map(Tag::getName).collect(Collectors.joining(", "));
        model.addAttribute("tags", tagString);
        model.addAttribute("view", "article/edit");
        model.addAttribute("article", article);


        return "base-layout";
    }

    @PostMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editProcess(@PathVariable Integer id, ArticleBindingModel articleBindingModel) {

        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.findById(id).get();
        Category category = this.categoryRepository.findById(articleBindingModel.getCategoryId()).get();
        HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());
        article.setTags(tags);
        article.setCategory(category);
        article.setContent(articleBindingModel.getContent());
        article.setTitle(articleBindingModel.getTitle());

        String originalNameAndFolder = article.getImagePath();

        MultipartFile file = articleBindingModel.getPicture();

        uploadFile(article, file);

        AlbumController albumController = new AlbumController();

        albumController.deleteFile(originalNameAndFolder);

        this.articleRepository.saveAndFlush(article);

        return "redirect:/article/" + article.getId();
    }

    @GetMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(Model model, @PathVariable Integer id) {

        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        model.addAttribute("article", article);
        model.addAttribute("view", "article/delete");

        return "base-layout";
    }

    @PostMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteProcess(@PathVariable Integer id) {

        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        String originalNameAndFolder = article.getImagePath();

        AlbumController albumController = new AlbumController();

        albumController.deleteFile(originalNameAndFolder);

        this.articleRepository.delete(article);

        return "redirect:/";
    }

    private boolean isUserAuthorOrAdmin(Article article) {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());

        return userEntity.isAdmin() || userEntity.isAuthor(article);
    }

    private HashSet<Tag> findTagsFromString(String tagString){
        HashSet<Tag> tags = new HashSet<>();
        String[] tagNames = tagString.split(",\\s*");

        for(String tagName : tagNames){
            Tag currentTag = this.tagRepository.findByName(tagName);
            if(currentTag==null){
                currentTag = new Tag(tagName);
                this.tagRepository.saveAndFlush(currentTag);
            }

            tags.add(currentTag);
        }
        return tags;
    }

    @GetMapping("/findArticle")
    public String findArticle(Model model){
        model.addAttribute("view", "findArticle/search");
        return "base-layout";
    }

    @PostMapping("/findArticle")
    public String findArticleProcess(ArticleBindingModel articleBindingModel){

        String searched = articleBindingModel.getTitle();

        List<Article> articles = this.articleRepository.findAll();

        for (Article article : articles) {
            if (ArticleTitleContainsSearched(searched, article.getTitle())){
                found.add(article);
            }
        }

        return "redirect:/article/viewFoundArticles";
    }

    private boolean ArticleTitleContainsSearched(String searched, String title) {
        return title.toLowerCase().contains(searched.toLowerCase());
    }

    @GetMapping("/article/viewFoundArticles")
    public String listFoundArticles(Model model){
        if (found.size() > 0){
            model.addAttribute("articles", found);
            model.addAttribute("view", "/findArticle/index");
            found = new LinkedList<>();
            return "base-layout";
        }
        return "findArticle/notFound";
    }
}

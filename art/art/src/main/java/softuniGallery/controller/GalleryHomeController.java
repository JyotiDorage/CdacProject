package softuniGallery.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import softuniGallery.bindingModel.AlbumBindingModel;
import softuniGallery.entity.*;
import softuniGallery.repository.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class GalleryHomeController {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LinkCategoryRepository linkCategoryRepository;
    @Autowired
    private AlbumCategoryRepository albumCategoryRepository;

    @GetMapping("/")
    public String index(Model model) {

        List<Category> categories = this.categoryRepository.findAll();
        List<LinkCategory> linkCategories = this.linkCategoryRepository.findAll();
        List<AlbumCategory> albumCategories = this.albumCategoryRepository.findAll();

        List<User> latestFiveUsers = this.userRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("latestFiveUsers", latestFiveUsers);
        model.addAttribute("view", "home/index");
        model.addAttribute("categories", categories);
        model.addAttribute("linkCategories", linkCategories);
        model.addAttribute("albumCategories", albumCategories);

        return "base-layout";
    }
    @GetMapping("/category/{id}")
    public String listArticles(Model model, @PathVariable Integer id){
        model.addAttribute("view", "home/list-articles");
        if(!this.categoryRepository.existsById(id)){
            return "redirect:/";
        }

        Category category = this.categoryRepository.findById(id).get();
        Set<Article> articles = category.getArticles();
        model.addAttribute("articles", articles);
        model.addAttribute("category", category);
        return "base-layout";
    }

    @GetMapping("/linkCategory/{id}")
    public String listLinks(Model model, @PathVariable Integer id){

        model.addAttribute("view", "home/list-links");

        if(!this.linkCategoryRepository.existsById(id)){
            return "redirect:/";
        }

        LinkCategory linkCategory = this.linkCategoryRepository.findById(id).get();
        Set<Link> links = linkCategory.getLinks();

        model.addAttribute("links", links);
        model.addAttribute("linkCategory", linkCategory);

        return "base-layout";
    }

    @GetMapping("/albumCategory/{id}")
    public String listAlbums(Model model, @PathVariable Integer id){

        model.addAttribute("view", "home/list-albums");

        if (!this.albumCategoryRepository.existsById(id)){
            return "redirect:/";
        }

        AlbumCategory albumCategory = this.albumCategoryRepository.findById(id).get();
        Set<Album> albums = albumCategory.getAlbums();

        model.addAttribute("albums", albums);
        model.addAttribute("albumCategory", albumCategory);

        return "base-layout";
    }

    @RequestMapping("/error/403")
    public String accessDenied(Model model) {
        model.addAttribute("view", "error/403");

        return "base-layout";
    }

    @RequestMapping("/error/404")
    public String notFound(Model model) {
        model.addAttribute("view", "error/404");

        return "base-layout";
    }


}

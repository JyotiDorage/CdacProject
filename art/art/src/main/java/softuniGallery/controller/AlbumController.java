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
import softuniGallery.bindingModel.AlbumBindingModel;
import softuniGallery.entity.*;
import softuniGallery.repository.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Controller
public class AlbumController {
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private AlbumCategoryRepository albumCategoryRepository;
    @Autowired
    private AlbumTagRepository albumTagRepository;

    private List<Album> found = new LinkedList<>();

    @GetMapping("/album/createAlbum")
    @PreAuthorize("isAuthenticated()")
    public String create(Model model) {
        List<AlbumCategory> albumCategories = this.albumCategoryRepository.findAll();

        model.addAttribute("linkCategories", albumCategories);
        model.addAttribute("view", "album/createAlbum");

        return "base-layout";
    }


    @PostMapping("/album/createAlbum")
    @PreAuthorize("isAuthenticated()")
    public String createProcess(AlbumBindingModel albumBindingModel, HttpServletRequest servletRequest) {

        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());

        AlbumCategory albumCategory= this.albumCategoryRepository.findById(albumBindingModel.getCategoryId()).get();

        HashSet<AlbumTag> albumTags = this.findTagsFromString(albumBindingModel.getTagString());

        Album albumEntity = new Album(
                albumBindingModel.getName(),
                userEntity,
                albumCategory,
                albumTags
        );

        List<MultipartFile> files = albumBindingModel.getPictures();
        List<ImageAlbum> imageAlbumList = new LinkedList<>();
        albumEntity.setAlbumPicture("Some text");
        this.albumRepository.saveAndFlush(albumEntity);

        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {

                ImageAlbum imageAlbum = new ImageAlbum();

                boolean setAlbumPicture = false;

                if (i == 0) {
                    setAlbumPicture = true;
                }
                if (files.get(i) != null) {
                    try {
                        String originalName = files.get(i).getOriginalFilename();
                        File imageFile = new File("C:\\Users\\George-Lenovo\\Desktop\\TeamProjectGallery\\gallery\\src\\main\\resources\\static\\images", originalName);
                        files.get(i).transferTo(imageFile);
                        String image = "/images/" + originalName;
                        imageAlbum.setPath(image);
                        imageAlbum.setAlbum(albumEntity);
                        this.imageRepository.saveAndFlush(imageAlbum);
                        imageAlbumList.add(imageAlbum);

                        if (setAlbumPicture) {
                            albumEntity.setAlbumPicture(image);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        albumEntity.setImageAlbums(imageAlbumList);

        this.albumRepository.saveAndFlush(albumEntity);
        return "redirect:/album/viewAlbums";
    }

    private HashSet<AlbumTag> findTagsFromString(String tagString) {

        HashSet<AlbumTag> albumTags = new HashSet<>();

        String[] tagNames = tagString.split(",\\s*");

        for (String tagName : tagNames) {
            AlbumTag currentTag = this.albumTagRepository.findByName(tagName);

            if (currentTag == null) {
                currentTag = new AlbumTag(tagName);
                this.albumTagRepository.saveAndFlush(currentTag);
            }

            albumTags.add(currentTag);
        }
        return albumTags;
    }

    @GetMapping("/album/viewAlbums")
    public String listAlbums(Model model) {
        List<Album> albums = this.albumRepository.findAll();

        model.addAttribute("albums", albums);
        model.addAttribute("view", "/album/indexAlbum");

        return "base-layout";
    }

    @GetMapping("/album/{id}")
    public String details(Model model, @PathVariable Integer id) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
            UserDetails principal = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User entityUser = this.userRepository.findByEmail(principal.getUsername());

            model.addAttribute("user", entityUser);
        }

        Album album = this.albumRepository.findById(id).get();

        model.addAttribute("album", album);
        model.addAttribute("view", "album/details");

        return "base-layout";
    }

    @GetMapping("/album/editName/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editName(@PathVariable Integer id, Model model) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + id;
        }

        String tagString = album.getAlbumTags().stream().map(AlbumTag::getName).collect(Collectors.joining(", "));

        List<AlbumCategory> categories = this.albumCategoryRepository.findAll();
        model.addAttribute("categories", categories);

        model.addAttribute("tags", tagString);
        model.addAttribute("view", "album/editName");
        model.addAttribute("album", album);

        return "base-layout";
    }

    @PostMapping("/album/editName/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editNameProcess(@PathVariable Integer id, AlbumBindingModel albumBindingModel) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + id;
        }

        String name = albumBindingModel.getName();

        album.setName(name);

        HashSet<AlbumTag> tags = this.findTagsFromString(albumBindingModel.getTagString());
        album.setAlbumTags(tags);

        AlbumCategory category = this.albumCategoryRepository.findById(albumBindingModel.getCategoryId()).get();
        album.setAlbumCategory(category);

        this.albumRepository.saveAndFlush(album);

        return "redirect:/album/" + album.getId();
    }

    @GetMapping("/album/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(Model model, @PathVariable Integer id) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + id;
        }

        model.addAttribute("album", album);
        model.addAttribute("view", "album/delete");

        return "base-layout";
    }

    @PostMapping("/album/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteProcess(@PathVariable Integer id, AlbumBindingModel albumBindingModel) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album" + id;
        }

        List<ImageAlbum> imageAlbumList = album.getImageAlbums();

        for (int i = 0; i < imageAlbumList.size(); i++) {

            ImageAlbum imageAlbum = imageAlbumList.get(i);
            String originalNameAndFolder = imageAlbum.getPath();

            this.imageRepository.delete(imageAlbumList.get(i));

            deleteFile(originalNameAndFolder);
        }

        this.albumRepository.delete(album);

        return "redirect:/album/viewAlbums";
    }

    public void deleteFile(String originalNameAndFolder) {
        try {
            File imageFile = new File("C:\\Users\\George-Lenovo\\Desktop\\TeamProjectGallery\\gallery\\src\\main\\resources\\static" + originalNameAndFolder);
            if (imageFile.delete()) {
                System.out.println(imageFile.getName() + " is deleted!");
            } else {
                System.out.println("Delete operation is failed!");
            }
        } catch (Exception ex) {
            System.out.println("Failed to delete image!");
        }
    }

    private boolean isUserAuthorOrAdmin(Album album) {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());

        return userEntity.isAdmin() || userEntity.isAuthor(album);
    }

    @GetMapping("/album/addPicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String addPicture(@PathVariable Integer id, Model model) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + id;
        }

        model.addAttribute("view", "album/addPicture");
        model.addAttribute("album", album);

        return "base-layout";
    }

    @PostMapping("/album/addPicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String addProcess(@PathVariable Integer id, AlbumBindingModel albumBindingModel) {
        if (!this.albumRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        Album album = this.albumRepository.findById(id).get();
        List<ImageAlbum> imageAlbumList = album.getImageAlbums();
        ImageAlbum imageAlbum = new ImageAlbum();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + id;
        }

        MultipartFile file = albumBindingModel.getPicture();

        if (file != null) {

            try {
                String originalName = file.getOriginalFilename();
                File imageFile = new File("C:\\Users\\George-Lenovo\\Desktop\\TeamProjectGallery\\gallery\\src\\main\\resources\\static\\images", originalName);
                file.transferTo(imageFile);
                String pathPicture = "/images/" + originalName;
                imageAlbum.setPath(pathPicture);
                imageAlbum.setAlbum(album);
                this.imageRepository.saveAndFlush(imageAlbum);
                imageAlbumList.add(imageAlbum);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        album.setImageAlbums(imageAlbumList);

        this.albumRepository.saveAndFlush(album);

        return "redirect:/album/" + album.getId();
    }

    @GetMapping("/album/editPicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editPicture(@PathVariable Integer id, Model model) {
        if (!this.imageRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        ImageAlbum image = this.imageRepository.findById(id).get();
        Album album = image.getAlbum();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + album.getId();
        }

        model.addAttribute("view", "album/editPicture");
        model.addAttribute("image", image);
        model.addAttribute("album", album);

        return "base-layout";
    }

    @PostMapping("/album/editPicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editPictureProcess(@PathVariable Integer id, AlbumBindingModel albumBindingModel) {
        if (!this.imageRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        ImageAlbum imageAlbum = this.imageRepository.findById(id).get();
        Album album = imageAlbum.getAlbum();
        String originalNameAndFolder = imageAlbum.getPath();
        String albumPicturePath = album.getAlbumPicture();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + album.getId();
        }

        List<MultipartFile> files = albumBindingModel.getPictures();

        editImage(imageAlbum, album, albumPicturePath, files);

        deleteFile(originalNameAndFolder);

        this.imageRepository.saveAndFlush(imageAlbum);

        return "redirect:/album/" + album.getId();
    }

    public void editImage(ImageAlbum imageAlbum, Album album, String albumPicturePath, List<MultipartFile> files) {
        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i) != null) {
                    try {
                        String originalName = files.get(i).getOriginalFilename();
                        File imageFile = new File("C:\\Users\\George-Lenovo\\Desktop\\TeamProjectGallery\\gallery\\src\\main\\resources\\static\\images", originalName);
                        files.get(i).transferTo(imageFile);
                        String pathPicture = "/images/" + originalName;

                        if (albumPicturePath.equals(imageAlbum.getPath())) {
                            album.setAlbumPicture(pathPicture);
                            this.albumRepository.saveAndFlush(album);
                        }

                        imageAlbum.setPath(pathPicture);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @GetMapping("/album/deletePicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deletePicture(@PathVariable Integer id, Model model) {
        if (!this.imageRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        ImageAlbum image = this.imageRepository.findById(id).get();
        Album album = image.getAlbum();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + album.getId();
        }

        model.addAttribute("view", "album/deletePicture");
        model.addAttribute("image", image);
        model.addAttribute("album", album);

        return "base-layout";
    }

    @PostMapping("/album/deletePicture/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deletePictureProcess(@PathVariable Integer id, AlbumBindingModel albumBindingModel) {
        if (!this.imageRepository.existsById(id)) {
            return "redirect:/album/viewAlbums";
        }

        ImageAlbum image = this.imageRepository.findById(id).get();
        Album album = image.getAlbum();
        String originalNameAndFolder = image.getPath();

        if (!isUserAuthorOrAdmin(album)) {
            return "redirect:/album/" + album.getId();
        }

        deleteFile(originalNameAndFolder);

        this.imageRepository.delete(image);

        return "redirect:/album/" + album.getId();
    }

    @GetMapping("/imageDetails/{id}")
    public String pictureDetails(@PathVariable Integer id, Model model) {
        ImageAlbum image = this.imageRepository.findById(id).get();
        Album album = image.getAlbum();

        if (!this.imageRepository.existsById(id)) {
            return "redirect:/album/" + album.getId();
        }

        model.addAttribute("view", "album/pictureDetails");
        model.addAttribute("image", image);
        model.addAttribute("album", album);

        return "base-layout";
    }

    @GetMapping("/find")
    public String findAlbum(Model model) {
        model.addAttribute("view", "findAlbum/search");
        return "base-layout";
    }

    @PostMapping("/find")
    public String findAlbumProcess(AlbumBindingModel albumBindingModel, Model model) {
        String searched = albumBindingModel.getName();
        List<Album> allAlbums = this.albumRepository.findAll();

        for (Album current : allAlbums) {
            if (AlbumNameContainsSearched(searched, current.getName())) {
                found.add(current);
            }
        }

        return "redirect:/album/viewFoundAlbums";
    }

    private boolean AlbumNameContainsSearched(String searched, String current) {
        return current.toLowerCase().contains(searched.toLowerCase());
    }

    @GetMapping("/album/viewFoundAlbums")
    public String listFoundAlbums(Model model) {
        if (found.size() > 0) {
            model.addAttribute("albums", found);
            model.addAttribute("view", "/findAlbum/index");
            found = new LinkedList<>();
            return "base-layout";
        }

        return "findAlbum/notFound";
    }
}
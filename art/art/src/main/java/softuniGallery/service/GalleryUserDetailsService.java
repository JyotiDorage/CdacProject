package softuniGallery.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import softuniGallery.entity.User;
import softuniGallery.repository.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service("galleryUserDetailsService")
public class GalleryUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public GalleryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid User");
        } else {
            Set<GrantedAuthority> grantedAuthorities = user.getRoles()
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toSet());

            return new org
                    .springframework
                    .security
                    .core
                    .userdetails
                    .User(user.getEmail(), user.getPassword(), grantedAuthorities);
        }
    }
}

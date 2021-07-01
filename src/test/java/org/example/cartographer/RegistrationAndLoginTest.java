package org.example.cartographer;

import org.example.cartographer.domain.User;
import org.example.cartographer.repos.UserRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RegistrationAndLoginTest {
    String TOKEN_ATTR_NAME = "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @Test
    public void loginAccessAllowedTest() throws Exception {
        this.mockMvc.perform(get("/login"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void correctLoginTest() throws Exception {
        this.mockMvc.perform(formLogin().user("testuser").password("testpassword"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void loginBadCredentialsTest() throws Exception {
        this.mockMvc.perform(formLogin().user("wronguser").password("wrongpassword"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    public void registrationAccessAllowedTest() throws Exception {
        this.mockMvc.perform(get("/registration"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void correctRegistrationTest() throws Exception {
        HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfToken csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());

        this.mockMvc.perform(post("/registration")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken)
                .param(csrfToken.getParameterName(), csrfToken.getToken())
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .param("_csrf", csrfToken.getToken())
                .param("username", "testCreatingUser_Name")
                .param("password", "testCreatingUser_Password")
                .param("passwordConfirm", "testCreatingUser_Password"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User user = userRepo.findByUsername("testCreatingUser_Name");
        if (user != null) {
            userRepo.deleteById(user.getId());
        }
    }

    @Test
    public void userExistRegistrationErrorTest() throws Exception {
        HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfToken csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());

        this.mockMvc.perform(post("/registration")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken)
                .param(csrfToken.getParameterName(), csrfToken.getToken())
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .param("_csrf", csrfToken.getToken())
                .param("username", "testuser")
                .param("password", "testCreatingUser_Password")
                .param("passwordConfirm", "testCreatingUser_Password"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Пользователь уже существует")));
    }

    @Test
    public void passwordMismatchRegistrationErrorTest() throws Exception {
        HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfToken csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());

        this.mockMvc.perform(post("/registration")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken)
                .param(csrfToken.getParameterName(), csrfToken.getToken())
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .param("_csrf", csrfToken.getToken())
                .param("username", "new_test_user")
                .param("password", "userPassword")
                .param("passwordConfirm", "incorrectUserPassword"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Пароли не совпадают")));
    }
}

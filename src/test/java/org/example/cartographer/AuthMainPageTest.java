package org.example.cartographer;

import org.example.cartographer.domain.Route;
import org.example.cartographer.repos.RouteRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("testuser")
public class AuthMainPageTest {
    String TOKEN_ATTR_NAME = "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN";

    @Autowired
    private RouteRepo routeRepo;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void accessOkTest() throws Exception {
        this.mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(authenticated());
    }

    @Test
    public void saveRouteStatusTests() throws Exception {
        saveRouteRequest("testroutename", "10.23,31.2", "ok");
        saveRouteRequest("testroutename", "10.23,31.2", "error3");
        for (Route route : routeRepo.findByName("testroutename")) {
            if (route != null) {
                routeRepo.deleteById(route.getId());
            }
        }
        saveRouteRequest("", "10.23,31.2", "error2");
        saveRouteRequest("testroutename", "", "error1");
    }

    public void saveRouteRequest(String routeName, String userPointList, String expectedContent) throws Exception {
        HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfToken csrfToken = httpSessionCsrfTokenRepository.generateToken(new MockHttpServletRequest());

        this.mockMvc.perform(post("/requests/saveRoute")
                .sessionAttr(TOKEN_ATTR_NAME, csrfToken)
                .param(csrfToken.getParameterName(), csrfToken.getToken())
                .param("routeName", routeName)
                .param("routeNote", "note")
                .flashAttr("username", "testuser")
                .flashAttr("userPointList", userPointList))
                .andDo(print())
                .andExpect(authenticated())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedContent));
    }
}

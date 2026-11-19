package org.grutzmann.movie100.springserver

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SpringserverApplicationTests {

    @Autowired
    private lateinit var mockMvc :MockMvc

    @Test
    fun contextLoads() {
    }

    @Test
    fun testLoginWithPath() {
        mockMvc.perform(post("/login/melli")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"login": "melli", "password": "geheim"}""")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.userId").isString)
            .andExpect(jsonPath("$.movies").isArray)
    }

    @Test
    fun testLoginWithBody() {
        mockMvc.perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"login": "melli", "password": "geheim"}""")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.userId").isString)
            .andExpect(jsonPath("$.movies").isArray)
    }
}

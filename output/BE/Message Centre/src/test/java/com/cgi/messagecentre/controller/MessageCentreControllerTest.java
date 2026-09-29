package com.cgi.messagecentre.controller; import org.junit.jupiter.api.Test; import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; import org.springframework.boot.test.mock.mockito.MockBean;
@WebMvcTest(MessageCentreController.class) class MessageCentreControllerTest { @MockBean MessageCentreService messageCentreService; @Test void controllerLoads(){} }

package com.cgi.messagecentre.controller;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/t-message/api") @Slf4j @RequiredArgsConstructor
public class MessageCentreController { private final MessageCentreService service;
@GetMapping("/t-message/api/v2/messagecentre:") public Object getTMessageApiV2Messagecentre(){ return null; }
@GetMapping("/t-message/api/v2/messagecentre/{messageId}:") public Object getTMessageApiV2MessagecentreMessageId(){ return null; }
@PostMapping("/t-message/api/v2/messagecentre/delete:") public Object postTMessageApiV2MessagecentreDelete(){ return null; }
@GetMapping("/t-message/api/v2/messagecentre/count:") public Object getTMessageApiV2MessagecentreCount(){ return null; }

}

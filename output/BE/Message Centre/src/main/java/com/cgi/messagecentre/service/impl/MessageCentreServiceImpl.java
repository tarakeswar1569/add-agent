package com.cgi.messagecentre.service.impl;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.stereotype.Service;
@Service @Slf4j @RequiredArgsConstructor
public class MessageCentreServiceImpl implements MessageCentreService {
  Object getTMessageApiV2Messagecentre();
  Object getTMessageApiV2MessagecentreMessageId();
  Object postTMessageApiV2MessagecentreDelete();
  Object getTMessageApiV2MessagecentreCount();

}

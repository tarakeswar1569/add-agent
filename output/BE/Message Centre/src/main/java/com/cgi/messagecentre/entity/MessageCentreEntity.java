package com.cgi.messagecentre.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="s") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageCentreEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Version private Long version; }

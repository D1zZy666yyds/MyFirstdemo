package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String salt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
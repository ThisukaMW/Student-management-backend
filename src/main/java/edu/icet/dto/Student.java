package edu.icet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Student {

    private String firstName;
    private String lastName;
    private int age;
    private String email;
    private String password;
    private String institute;
    private String batch;
    private String gender;



}


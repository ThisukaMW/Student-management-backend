package edu.icet.controller;

import edu.icet.entity.Student;
import edu.icet.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@Slf4j
@RequestMapping("/information")

public class StudentController {
    @Autowired
    StudentService service;

    @PostMapping
    public void createStudent(@RequestBody edu.icet.dto.Student student){
        service.createStudent(student);
    }

    @GetMapping("/{email}/{password}")
    public boolean getEmailAndPassword(@PathVariable String email,@PathVariable String password){
        boolean emailAndPassWord = service.getEmailAndPassword(email,password);
        return emailAndPassWord;
    }

    @GetMapping
    public Iterable<Student> getAll(){
       return service.getAll();
    }
}






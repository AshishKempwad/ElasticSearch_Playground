package com.ashish.elasticsearch_playground.sec02;

import com.ashish.elasticsearch_playground.AbstractTest;
import com.ashish.elasticsearch_playground.sec02.entity.Employee;
import com.ashish.elasticsearch_playground.sec02.repository.EmployeeRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.stream.IntStream;

public class CrudOperationsTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrudOperationsTest.class);

    @Autowired
    private EmployeeRepository repository;

    @Test
    public void crud(){
        var employee = this.createEmployee(1,"Sam",30);

        //save
        this.repository.save(employee);
        this.printAll();

        //find by id
        employee = this.repository.findById(1).orElseThrow();
        Assertions.assertEquals(30,employee.getAge());
        Assertions.assertEquals("Sam",employee.getName());

        //update and save
        employee.setAge(32);
        employee = this.repository.save(employee);
        this.printAll();
        Assertions.assertEquals(32,employee.getAge());

        //delete
        this.repository.deleteById(1);
        this.printAll();
        Assertions.assertTrue(this.repository.findById(1).isEmpty());

    }

    @Test
    public void bulkCrud(){
        var list = IntStream.rangeClosed(1,10)
                .mapToObj(i->this.createEmployee(i,"name-"+i, 30+i))
                .toList();
        this.repository.saveAll(list);
        this.printAll();

        //check count
        Assertions.assertEquals(10,this.repository.count());

        //find by ids
        var ids = List.of(2,4,6);
        var iterable = this.repository.findAllById(ids);
        list = Streamable.of(iterable).toList();
        Assertions.assertEquals(3,list.size());

        //update and save
        list.forEach(e->e.setAge(e.getAge()+10)); // mutable object
        this.repository.saveAll(list);
        this.printAll();
        this.repository.findAllById(ids)
                .forEach(e->Assertions.assertEquals(e.getId()+40,e.getAge()));

        //delete some ids and check the count
        this.repository.deleteAllById(ids);
        this.printAll();
        Assertions.assertEquals(7,this.repository.count());
    }

    private Employee createEmployee(int id, String name, int age){
        var employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setAge(age);

        return employee;
    }

    private void printAll(){
        this.repository.findAll()
                .forEach(e -> log.info("employee: {}",e));
    }
}

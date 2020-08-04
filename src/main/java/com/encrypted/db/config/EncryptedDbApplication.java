package com.encrypted.db.config;

import com.encrypted.db.entity.Account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan(basePackages="com.encrypted.db")
@SpringBootApplication
@EnableJpaRepositories("com.encrypted.db.dao")
@EntityScan(basePackageClasses= Account.class)
public class EncryptedDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(EncryptedDbApplication.class, args);
	}

}

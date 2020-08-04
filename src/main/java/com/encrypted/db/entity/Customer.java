package com.encrypted.db.entity;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer implements Serializable {

    private static final long serialVersionUID = -7105027048257651307L;

    private int id;
    private String ssn;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private List<Account> accounts;


    @Override
    public String toString() {
        return new StringBuilder("Customer\n")
                .append("id: ").append(id).append("\n")
                .append("ssn: ").append(ssn).append("\n")
                .append("Name: ").append(firstName).append(" ").append(middleInitial).append(" ").append(lastName).append("\n")
                .append("accounts: ").append(accounts)
                .toString();
    }

}

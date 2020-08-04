package com.encrypted.db.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account implements Serializable {

    private static final long serialVersionUID = -1402490583550576686L;

    int id;
    int customerId;
    String accountNumber;
    double balance;

    @Override
    public String toString() {
        return new StringBuilder("\n\tAccount\n")
                .append("\t\tid: ").append(id).append("\n")
                .append("\t\tCustomer Id: ").append(customerId).append("\n")
                .append("\t\tAccount Number: ").append(accountNumber).append("\n")
                .append("\t\tBalance: ").append(balance)
                .toString();
    }
}

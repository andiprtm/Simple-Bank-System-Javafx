package com.bank.app.models;

import com.bank.app.ConnectionManager;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.sql.*;

public class Customer {
    /*Limitasi customer*/
    BigDecimal maximumTransfer;
    BigDecimal maximumDeposit;
    BigDecimal maximumWithdraw;
    double adminFee;

    /*Customer Data*/
    public Integer customerId;
    public String accountType;
    public Boolean isActive;
    public Integer pin;
    public String username;
    public String password;
    public String name;
    public String address;
    public String phone;
    public BigDecimal balance;
    Connection conn = ConnectionManager.getInstance().getConnection();

    public Customer(String username, String password){
        this.username = username;
        this.password = password;
    }

    public void authenticate() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    """
                            SELECT cba.customer_id, cba.customer_is_active,
                                   (SELECT customer_account_type FROM customer_account_type WHERE id_customer_account_type=cba.customer_account_type_id) as account_type
                            FROM customer_data cd, customer_bank_account cba
                            WHERE username=? AND password=? AND cba.customer_id=cd.id_customer;"""
            );

            ps.setString(1, this.username);
            ps.setString(2, this.password);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.customerId = rs.getInt("customer_id");
                this.isActive = rs.getBoolean("customer_is_active");
                this.accountType = rs.getString("account_type");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getCustomerData(){
        try {
            PreparedStatement ps = conn.prepareStatement("""
                    SELECT cd.username, cd.password, cd.name, cd.phone, cd.address,
                        cba.account_balance, cba.customer_is_active, cba.account_pin,
                        cat.customer_account_type, cat.max_balance_limit, cat.max_transfer_limit, cat.max_withdraw_limit, cat.max_deposit_limit, cat.admin_fee
                    FROM customer_data cd, customer_bank_account cba, customer_account_type cat
                    WHERE cd.id_customer=? AND cba.customer_id=cd.id_customer AND cat.id_customer_account_type=cba.customer_account_type_id""");

            ps.setInt(1, this.customerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.username = rs.getString("username");
                this.password = rs.getString("password");
                this.name = rs.getString("name");
                this.address = rs.getString("address");
                this.phone = rs.getString("phone");
                this.balance = rs.getBigDecimal("account_balance");
                this.isActive = rs.getBoolean("customer_is_active");
                this.pin = rs.getInt("account_pin");
                this.accountType = rs.getString("customer_account_type");
                this.maximumWithdraw = rs.getBigDecimal("max_withdraw_limit");
                this.maximumTransfer = rs.getBigDecimal("max_transfer_limit");
                this.maximumDeposit = rs.getBigDecimal("max_deposit_limit");
                this.adminFee = rs.getDouble("admin_fee");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getCustomerId(String username) {
        Integer customerId = null;

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT cd.id_customer FROM customer_data cd WHERE cd.username=?;"
            );

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                customerId = rs.getInt("id_customer");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return customerId;
    }

    public BigDecimal updatedBalance() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT cba.account_balance FROM customer_bank_account cba WHERE cba.customer_id=?;"
            );

            ps.setInt(1, this.customerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.balance = rs.getBigDecimal("account_balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return this.balance;
    }

    public Boolean getIsActive(Integer customerId) {
        Boolean isActive = null;

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT cba.customer_is_active FROM customer_bank_account cba WHERE cba.customer_id=?;"
            );

            ps.setInt(1, customerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                isActive = rs.getBoolean("customer_is_active");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return isActive;
    }

    public BigDecimal getBalanceAfterAddition(Integer customerId, BigDecimal amount) {
        BigDecimal remainingBalance = null;

        try {
            PreparedStatement ps = conn.prepareStatement("""
                    SELECT IF((cba.account_balance + ?) > cat.max_balance_limit = 1, -1, cba.account_balance + ?)
                        AS remaining_balance
                    FROM customer_bank_account cba, customer_account_type cat
                    WHERE cba.customer_id=? AND cat.id_customer_account_type=cba.customer_account_type_id;""");

            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, customerId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                remainingBalance = rs.getBigDecimal("remaining_balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return remainingBalance;
    }

    public BigDecimal getBalanceAfterSubtraction(Integer customerId, BigDecimal amount) {
        BigDecimal remainingBalance = null;

        try {
            PreparedStatement ps = conn.prepareStatement(
                    """
                            SELECT IF(SIGN(cba.account_balance - ?) = -1, -1, cba.account_balance - ?)
                                AS remaining_balance
                            FROM customer_bank_account cba
                            WHERE cba.customer_id=?;"""
            );

            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, customerId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                remainingBalance = rs.getBigDecimal("remaining_balance");

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return remainingBalance;
    }

    public void updateBalance (Integer customerId, BigDecimal updatedBalance) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE customer_bank_account SET account_balance=? WHERE customer_id=?;"
            );

            ps.setBigDecimal(1, updatedBalance);
            ps.setInt(2, customerId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean transferToAnotherBankAccount (String username, BigDecimal amount, Label label) {
        Boolean isSuccess = null;

        Integer receiverCustomerId = getCustomerId(username);
        if (receiverCustomerId == null) {
            label.setText("Rekening penerima tidak ditemukan!");
            isSuccess = false;
        } else {
            Boolean isReceiverAccountActive = getIsActive(receiverCustomerId);

            if (isReceiverAccountActive) {

                BigDecimal adminFeeTotal = amount.multiply(BigDecimal.valueOf(adminFee));
                BigDecimal amountAfterAdminFee = amount.add(adminFeeTotal);

                if (amountAfterAdminFee.compareTo(maximumTransfer) <= 0) {

                    BigDecimal remainingBalanceSender = this.getBalanceAfterSubtraction(this.customerId, amountAfterAdminFee);
                    BigDecimal remainingBalanceReceiver = this.getBalanceAfterAddition(receiverCustomerId, amountAfterAdminFee);

                    if (remainingBalanceSender.compareTo(BigDecimal.valueOf(0)) > 0 && remainingBalanceReceiver.compareTo(BigDecimal.valueOf(0)) > 0) {

                        updateBalance(this.customerId, remainingBalanceSender);
                        updateBalance(receiverCustomerId, remainingBalanceReceiver);
                        Transaction transaction = new Transaction();
                        transaction.createTransaction(receiverCustomerId, this.customerId, amount, "Transfer", true, "Sukses melakukan transfer!", adminFeeTotal);
                        System.out.println("Berhasil melakukan transfer " + amount  + " dengan biaya admin sebesar " + adminFeeTotal + " ke rekening " + username);
                        isSuccess = true;

                    } else if (remainingBalanceSender.compareTo(BigDecimal.valueOf(0)) < 0) {

                        Transaction transaction = new Transaction();
                        transaction.createTransaction(receiverCustomerId, this.customerId, amount, "Transfer", false, "Saldo anda tidak cukup untuk melakukan transfer sebesar " + amount + " + biaya admin sebesar " + adminFeeTotal, adminFeeTotal);
                        label.setText("Saldo anda tidak cukup untuk melakukan transfer sebesar " + amount + " + biaya admin sebesar " + adminFeeTotal);
                        isSuccess = true;

                    } else if (remainingBalanceReceiver.compareTo(BigDecimal.valueOf(0)) < 0) {

                        Transaction transaction = new Transaction();
                        transaction.createTransaction(receiverCustomerId, this.customerId, amount, "Transfer", false, "Rekening penerima telah mencapai batas saldo!", adminFeeTotal);
                        label.setText("Rekening penerima telah mencapai batas saldo!");
                        isSuccess = true;

                    }
                } else {

                    label.setText("Maksimal transfer adalah " + this.maximumTransfer);
                    isSuccess = false;

                }
            } else {

                label.setText("Username rekening penerima tidak aktif!");
                isSuccess = false;

            }
        }

        return isSuccess;
    }

    public void withdrawBalance (BigDecimal amount) {
        BigDecimal adminFeeTotal = amount.multiply(BigDecimal.valueOf(adminFee));
        BigDecimal amountAfterAdminFee = amount.add(adminFeeTotal);

        BigDecimal remainingBalance = this.getBalanceAfterSubtraction(this.customerId, amountAfterAdminFee);

        if (amount.compareTo(maximumWithdraw) <= 0) {

            if (remainingBalance.compareTo(BigDecimal.valueOf(0)) > 0) {

                updateBalance(this.customerId, remainingBalance);
                Transaction transaction = new Transaction();
                transaction.createTransaction(this.customerId, this.customerId, amount, "Withdraw", true, "Sukses melakukan penarikan!", adminFeeTotal);
                System.out.println("Berhasil melakukan penarikan sebesar " + amount + " dengan biaya admin sebesar " + adminFeeTotal);

            } else {

                Transaction transaction = new Transaction();
                transaction.createTransaction(this.customerId, this.customerId, amount, "Withdraw", false, "Saldo anda tidak cukup untuk melakukan penarikan sebesar " + amount + " + biaya admin sebesar " + adminFeeTotal, adminFeeTotal);
                System.out.println("Saldo anda tidak cukup untuk melakukan penarikan sebesar " + amount + " + biaya admin sebesar " + adminFeeTotal);

            }

        } else {

            System.out.println("Maksimal penarikan adalah " + this.maximumWithdraw);

        }
    }

    public void depositBalance (BigDecimal amount) {
        BigDecimal remainingBalance = this.getBalanceAfterAddition(this.customerId, amount);

        if (amount.compareTo(maximumDeposit) <= 0) {

            updateBalance(this.customerId, remainingBalance);
            Transaction transaction = new Transaction();
            transaction.createTransaction(this.customerId, this.customerId, amount, "Deposit", true, "Sukses melakukan deposit!", BigDecimal.valueOf(0));
            System.out.println("Sukses melakukan deposit sejumlah " + amount);

        } else {

            System.out.println("Maksimal deposit adalah " + this.maximumDeposit);

        }
    }
}


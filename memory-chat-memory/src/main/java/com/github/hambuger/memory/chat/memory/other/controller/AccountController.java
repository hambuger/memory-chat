package com.github.hambuger.memory.chat.memory.other.controller;

import com.github.hambuger.memory.chat.memory.other.account.AccountDTO;
import com.github.hambuger.memory.chat.memory.other.account.AccountManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AccountController {

    @Resource
    private AccountManager accountManager;

    @PostMapping("/register")
    @ResponseBody
    public AccountDTO registerAccount(@RequestBody AccountDTO accountDTO) {
        return accountManager.registerAccount(accountDTO);
    }

    @PostMapping("/login")
    @ResponseBody
    public AccountDTO loginAccount(@RequestBody AccountDTO accountDTO) {
        return accountManager.loginAccount(accountDTO);
    }



}

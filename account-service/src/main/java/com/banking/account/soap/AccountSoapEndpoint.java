package com.banking.account.soap;

import com.banking.account.dto.AccountResponseDto;
import com.banking.account.service.AccountService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Endpoint
public class AccountSoapEndpoint {

    private static final String NAMESPACE_URI = "http://banking.com/cbs/soap";
    private final AccountService accountService;

    public AccountSoapEndpoint(AccountService accountService) {
        this.accountService = accountService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetAccountBalanceRequest")
    @ResponsePayload
    public Element handleGetAccountBalance(@RequestPayload Element request) throws Exception {
        String accountNumber = request.getElementsByTagNameNS(NAMESPACE_URI, "accountNumber")
                .item(0).getTextContent();

        AccountResponseDto account = accountService.getAccountByNumber(accountNumber);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element response = doc.createElementNS(NAMESPACE_URI, "tns:GetAccountBalanceResponse");

        Element accNumEl = doc.createElementNS(NAMESPACE_URI, "tns:accountNumber");
        accNumEl.setTextContent(account.accountNumber());
        response.appendChild(accNumEl);

        Element balanceEl = doc.createElementNS(NAMESPACE_URI, "tns:availableBalance");
        balanceEl.setTextContent(account.availableBalance().toPlainString());
        response.appendChild(balanceEl);

        Element currEl = doc.createElementNS(NAMESPACE_URI, "tns:currency");
        currEl.setTextContent(account.currency());
        response.appendChild(currEl);

        Element statusEl = doc.createElementNS(NAMESPACE_URI, "tns:status");
        statusEl.setTextContent(account.status().name());
        response.appendChild(statusEl);

        return response;
    }
}

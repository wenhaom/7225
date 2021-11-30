package wenhao.demo.service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class RSA256Key {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public RSA256Key() {
    }

    public RSA256Key(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }
    public void setPublicKey(RSA256Key res256Key, RSAPublicKey publicKey){
        res256Key.publicKey=publicKey;
    }
    public void setPrivateKey(RSA256Key res256Key, RSAPrivateKey privateKey){
        res256Key.privateKey=privateKey;
    }
    public   RSAPrivateKey getRSAPrivateKey(){
        return this.privateKey;
    }


}
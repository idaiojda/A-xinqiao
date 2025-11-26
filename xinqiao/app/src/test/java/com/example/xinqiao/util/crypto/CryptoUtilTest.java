package com.example.xinqiao.util.crypto;

import org.junit.Assert;
import org.junit.Test;

public class CryptoUtilTest {
    @Test
    public void encryptDecryptWorks() {
        String src = "hello-敏感-123";
        String enc = CryptoUtil.encrypt(src);
        String dec = CryptoUtil.decrypt(enc);
        Assert.assertEquals(src, dec);
    }
}
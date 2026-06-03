package com.skillmatch.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyuncs.exceptions.ClientException;
import com.skillmatch.context.UserContext;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@SpringBootTest
class OssUtilTest {
    //private static final String OSS_KEY=202505/userId/fileName;
    @Test
   public void test() throws ClientException {
        String fileName="fileName.jpg";
        int lastDotIndex = fileName.lastIndexOf(".");
        String lastDot = fileName.substring(lastDotIndex+1);
        System.out.println(lastDot);
    }

}
package com.flyemu.share.aliyuncs.impl;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.GetObjectRequest;
import com.flyemu.share.aliyuncs.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
@Slf4j
public class OssServiceImpl implements OssService {

    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Value("${server.tomcat.basedir}")
    private String tmpdir;

    @Override
    public UploadBody upload(UploadBody body) {
        OSSClient ossClient = getClient();
        ossClient.putObject(body.getBucketName(), body.getObjectName(), body.getContent());
        ossClient.shutdown();
        body.setUrl("https://" + body.getBucketName() + "." + ENDPOINT + "/" + body.getObjectName());
        return body;
    }

    @Override
    public DownloadBody download(DownloadBody body) {
        OSSClient ossClient = getClient();
        File file = new File(tmpdir, body.getObjectName());
        ossClient.getObject(new GetObjectRequest(body.getBucketName(), body.getObjectName()), file);
        ossClient.shutdown();
        body.setContent(file);
        return body;
    }

    @Override
    public void delete(DownloadBody body) {
        OSSClient ossClient = getClient();
        ossClient.deleteObject(body.getBucketName(), body.getObjectName());
        ossClient.shutdown();
    }

    private OSSClient getClient() {
        log.info("accessKeyId:{},accessKeySecret:{}",accessKeyId,accessKeySecret);
        return new OSSClient(ENDPOINT, new DefaultCredentialProvider(accessKeyId, accessKeySecret), null);
    }
}

package com.flyemu.share.aliyuncs;

import lombok.Data;
import lombok.NonNull;

import java.io.File;
import java.io.InputStream;

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2019 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : 阿里云OSS云存储</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2019年01月14日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */

public interface OssService {

    String ENDPOINT = "oss-cn-hangzhou.aliyuncs.com";

    UploadBody upload(UploadBody body);

    DownloadBody download(DownloadBody body);

    void delete(DownloadBody body);

    @Data
    class UploadBody {
        @NonNull
        private String bucketName;

        @NonNull
        private InputStream content;
        /**
         * 一个文件存储的路径
         */
        @NonNull
        private String objectName;

        private String url;
    }

    @Data
    class DownloadBody {
        @NonNull
        private String bucketName;
        /**
         * 一个文件存储的路径
         */
        @NonNull
        private String objectName;

        private File content;
    }
}

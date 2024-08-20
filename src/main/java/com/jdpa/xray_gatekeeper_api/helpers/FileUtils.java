package com.jdpa.xray_gatekeeper_api.helpers;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileUtils {

    /**
     * Renames an array of MultipartFile objects by appending a UUID to each original filename.
     *
     * @param files Array of MultipartFile objects to be renamed.
     * @return List of MultipartFile objects with new names.
     */
    public static List<MultipartFile> renameFilesWithUUID(MultipartFile[] files){
        List<MultipartFile> renamedFiles = new ArrayList<>();
        try{
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename != null) {
                    String newFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                    MultipartFile renamedFile = new RenamedMultipartFile(file, newFilename);
                    renamedFiles.add(renamedFile);
                }
            }
        }catch(Exception e){
            System.out.println("Unable to rename files: " + e.getMessage());
            return List.of(files);
        }
        return renamedFiles;
    }

    /**
     * A wrapper class to rename MultipartFile objects.
     */
    private static class RenamedMultipartFile implements MultipartFile {
        private final MultipartFile originalFile;
        private final String newFilename;

        public RenamedMultipartFile(MultipartFile originalFile, String newFilename) throws IOException {
            this.originalFile = originalFile;
            this.newFilename = newFilename;
        }

        @Override
        public String getName() {
            return newFilename;
        }

        @Override
        public String getOriginalFilename() {
            return newFilename;
        }

        @Override
        public String getContentType() {
            return originalFile.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return originalFile.isEmpty();
        }

        @Override
        public long getSize() {
            return originalFile.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return originalFile.getBytes();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return originalFile.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            originalFile.transferTo(dest);
        }
    }
}

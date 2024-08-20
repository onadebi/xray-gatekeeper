package com.jdpa.xray_gatekeeper_api.xray.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Getter
@Setter
public class FilesTransferData {
    private Long id;
    private String token;
    private String projectKey;
    private String operation;
    private String url;
    private String message;
    private String[] fileNames;
    @JsonIgnore
    private Map<String, List<MultipartFile>> filesData;


    /**
     * Creates a new instance of FilesTransferData and populates it with the provided values.
     *
     * @param token      The authentication token associated with the file transfer. This is used to 
     *                   authorize the request.
     * @param filesData  A map containing lists of MultipartFile objects, where the key is a string identifier.
     * @param operation  A string representing the operation to be performed (e.g., PublishCucumberToXray, PublishFeatureFileToXray). This helps 
     *                   determine the context or purpose of the file transfer.
     * @param url      The Xray endpoint to call on REST API V2
     * @param message  Optional message
     * @param projectKey (Optional) The project key that is associated with the files. If provided, it is used 
     *                   to link the files to a specific project. If this is null or empty, the project key is not set.
     * 
     * @return A new instance of FilesTransferData populated with the provided token, filesData, operation, 
     *         and projectKey (if provided).
     */
    public FilesTransferData AddNew(String token, Map<String, List<MultipartFile>> filesData, String operation, String url, String message){
        FilesTransferData fileData = new FilesTransferData();
        fileData.setToken(token);
        fileData.setUrl(url);
        fileData.setMessage(message);
        fileData.setOperation(operation);
//        if(projectKey != null && !projectKey.isEmpty())
//        {
//            fileData.setProjectKey(projectKey);
//        }
        if(filesData != null && !filesData.isEmpty()){
            fileData.setFilesData(filesData);
            List<String> namesList = new ArrayList<>();
            for(Map.Entry<String, List<MultipartFile>> entry : filesData.entrySet()){
                for(MultipartFile file : entry.getValue()){
                    namesList.add(file.getOriginalFilename());
                }
            }
            fileData.setFileNames(namesList.toArray(new String[0]));
        }
        return fileData;
    }

//    public FilesTransferData AddNew(String token, Map<String, List<MultipartFile>> filesData, String operation, String projectKey, String message){
//        FilesTransferData fileData = new FilesTransferData();
//        fileData.setMessage(message);
//        fileData.setToken(token);
//        fileData.setOperation(operation);
//        if(projectKey != null && !projectKey.isEmpty())
//        {
//            fileData.setProjectKey(projectKey);
//        }
//        fileData.setFilesData(filesData);
//        return fileData;
//    }

    /**
     * Converts the FilesTransferData object to a JSON string representation.
     *
     * @return A JSON string representing the current state of the FilesTransferData object.
     */
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String objResp = "";
        try{
            objResp = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            objResp = "ERROR: "+e.getMessage();
        }
        return objResp;
    }

    /**
     * Converts a JSON string to a FilesTransferData object.
     *
     * @param jsonString The JSON string representing a FilesTransferData object.
     * @return A FilesTransferData object populated with data from the provided JSON string.
     */
    public static FilesTransferData fromJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        FilesTransferData objResp;
        try {
            objResp = objectMapper.readValue(jsonString, FilesTransferData.class);
        } catch (Exception e) {
            objResp = new FilesTransferData().AddNew(null, null,null,null,String.format("ERROR %s", e.getMessage()));
        }
        return objResp;
    }

}

package com.jdpa.xray_gatekeeper_api.xray.dtos;

public class AppResponse<T> {
    private T result;
    private String error;
    private int StatCode = 200;
    private boolean isSuccess;

    public AppResponse(T result, String error, int statCode, boolean isSuccess) {
        this.result = result;
        this.error = error;
        StatCode = statCode;
        this.isSuccess = isSuccess;
    }

    public AppResponse() {
    }

    public AppResponse(T input) {
    }

    public static <T> AppResponse<T> success(T result, int statCode){
        AppResponse<T> appResponse = new AppResponse<>();
        appResponse.setResult(result);
        appResponse.setStatCode(statCode);
        appResponse.setSuccess(true);
        return appResponse;
    }

    public static <T> AppResponse<T> failed(T result, String error, int statCode){
        AppResponse<T> appResponse = new AppResponse<>();
        appResponse.setResult(result);
        appResponse.setStatCode(statCode);
        appResponse.setError(error);
        appResponse.setSuccess(false);
        return appResponse;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatCode() {
        return StatCode;
    }

    public void setStatCode(int statCode) {
        StatCode = statCode;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}

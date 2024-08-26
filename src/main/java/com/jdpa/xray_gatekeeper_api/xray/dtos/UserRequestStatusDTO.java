package com.jdpa.xray_gatekeeper_api.xray.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class UserRequestStatusDTO {
    private LocalDateTime from;
    private LocalDateTime to;

    public UserRequestStatusDTO(Date from, Date to) {
        this.from = this.setToMidnight(from);
        this.to = this.setToMidnight(to);
    }

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

    //#region HELPERS
    private LocalDateTime setToMidnight(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    //#endregion
}

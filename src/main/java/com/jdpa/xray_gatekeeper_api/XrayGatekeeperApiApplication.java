package com.jdpa.xray_gatekeeper_api;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class XrayGatekeeperApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(XrayGatekeeperApiApplication.class, args);
	}

	@GetMapping
	public AppResponse<String> mainController() {
        return AppResponse.success("Service is up and running",200);
	}
}

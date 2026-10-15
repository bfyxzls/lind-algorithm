package com.lind.qps.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Web 层异常转页面提示。
 */
@ControllerAdvice
public class WebExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleBadRequest(IllegalArgumentException ex, Model model) {
		model.addAttribute("error", ex.getMessage());
		model.addAttribute("status", 400);
		return "error";
	}

}

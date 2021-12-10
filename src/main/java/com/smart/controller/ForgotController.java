package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {
	
	Random random = new Random();
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	//email id form open handler
	@RequestMapping("/forgot")
	public String openEmailForm()
	{
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session)
	{
		
		//generating otp
		int otp = random.nextInt(999999);
		
		//code to sent otp to email
		String subject="OTP from Smart-Contact-Manager";
		String message = ""
				+ "<div style='border: 1px solid #e2e2e2; padding: 20px;'>"
				+ "<h1>"
				+ "OTP is "
				+ "<b style='color: red;'><u>"+otp+"</u></b>"
				+ "</h1>"
				+ "</div>";
		String to = email;
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag)
		{
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		}
		else
		{
			session.setAttribute("message", "Check Your Email ID");
			return "forgot_email_form";
		}
 		
	}
	
	//verify-otp
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp, HttpSession session)
	{
		int myOtp = (int) session.getAttribute("myotp");
		String email = (String)session.getAttribute("email");
		if(myOtp==otp)
		{
			//password change form
			User user = this.userRepository.getUserByUsername(email);
			
			if(user==null)
			{
				session.setAttribute("message", "This user does not exist... Sign up to proceed");
				return "forgot_email_form";
			}
			else
			{
				return "password_change_form";
			}
			
		}
		else
		{
			session.setAttribute("message", "You have entered wrong OTP!!!");
			return "verify_otp";
		}
	}
	
	//change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword, HttpSession session)
	{
		String email = (String)session.getAttribute("email");
		User user = this.userRepository.getUserByUsername(email);
		user.setPassword(bCryptPasswordEncoder.encode(newPassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=password changed successfully";
	}
}

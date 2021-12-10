package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.razorpay.*;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal)
	{
		String userName =  principal.getName();
		User user= userRepository.getUserByUsername(userName);
		model.addAttribute("user", user);
	}
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal)
	{
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session)
	{
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUsername(name);
			
			//processing and uploading file...
			if(file.isEmpty())
			{
				System.out.println("File is empty");
				contact.setImage("default.jpg");
			}
			else
			{
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("/static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			//bidirectional mapping of user and contact
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);
			
			session.setAttribute("message", new Message("Contact Added Successfully", "success"));
			
		}
		catch(Exception e)
		{
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong!! Try again", "danger"));
		}
		return "normal/add_contact_form";
	}
	
	//show contacts handler
	//per page = 5 [n]
	//current page
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal)
	{
		m.addAttribute("title", "Show User Contacts");
		
		//method-1 to fetch user contacts
//		String userName = principal.getName();
//		User user = this.userRepository.getUserByUsername(userName);
//		List<Contact> contacts = user.getContacts();
		
		//method-2
		String userName = principal.getName();
		User user = this.userRepository.getUserByUsername(userName);
		
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing particular contact details
	@GetMapping("/contact/{cid}")
	public String showContactDetail(@PathVariable("cid") Integer cid, Model m, Principal principal)
	{
		//System.out.println(cid);
		Optional<Contact> contactOptional = this.contactRepository.findById(cid);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUsername(userName);
		
		if(user.getId()==contact.getUser().getId())
		{
			m.addAttribute("contact", contact);
			m.addAttribute("title", "Contact Details");
		}
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid, Model m, Principal principal, HttpSession session)
	{
		Optional<Contact> contactOptional = this.contactRepository.findById(cid);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUsername(userName);
		
		if(user.getId() == contact.getUser().getId())
		{
			contact.setUser(null);  //unlinking contact with user due to cascading
			this.contactRepository.delete(contact);
			session.setAttribute("message", new Message("Contact Successfully Deleted", "success"));
		}
		
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m)
	{
		m.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	//update contact handler
	@RequestMapping(value = "/process-update", method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model m, HttpSession session, Principal principal)
	{
		try 
		{
			Contact oldContactDetail = this.contactRepository.findById(contact.getCid()).get();
			
			if(!file.isEmpty())
			{
				//delete old photo
				File deleteFile = new ClassPathResource("/static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();
				
				//update new photo
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("/static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			else
			{
				contact.setImage(oldContactDetail.getImage());
			}
			User user = this.userRepository.getUserByUsername(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/contact/"+contact.getCid();
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model m)
	{
		m.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	//open settings handler
	@GetMapping("/settings")
	public String openSettings(Model m)
	{
		m.addAttribute("title", "Settings");
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession httpSession) 
	{
		String userName = principal.getName();
		User user = this.userRepository.getUserByUsername(userName);
		
		if(bCryptPasswordEncoder.matches(oldPassword, user.getPassword()))
		{
			//change the password
			user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(user);
			httpSession.setAttribute("message", new Message("Your password is successfully changed", "success"));
		}
		else
		{
			//error...
			httpSession.setAttribute("message", new Message("Please enter correct old password", "danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
	
	//creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception
	{
		System.out.println(data);
		int amount = Integer.parseInt(data.get("amount").toString());
		RazorpayClient razorpayClient = new RazorpayClient("rzp_test_UTT1lrHygK2ySp", "IVjU2EwLozkQrnXXWUddxE7d");
		
		JSONObject options = new JSONObject();
		options.put("amount", amount*100);
		options.put("currency", "INR");
		options.put("receipt", "txn_123456");
		
		//creating new order
		Order order = razorpayClient.Orders.create(options);
		System.out.println(order);
		
		//save order in database
		MyOrder myOrder = new MyOrder();
		
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUsername(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		
		
		
		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data){
		
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("order_status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		return ResponseEntity.ok(Map.of("msg", "payment details updated"));
	}
	
}

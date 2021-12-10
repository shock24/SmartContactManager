const toggleSidebar=()=>{
	
	if($(".sidebar").is(":visible")){
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
	}
	else{
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
	
};

const search=()=>{
	// console.log("searching...")

	let query = $("#search-input").val()
	if(query=='')
	{
		$(".search-result").hide();
	}
	else
	{
		//sending request to server
		let url=`http://localhost:8080/search/${query}`

		fetch(url).then((response)=>{
			return response.json();
		}).then((data)=>{
			let text=`<div class='list-group'>`

			data.forEach((contact)=>{
				text+=`<a href='/user/contact/${contact.cid}' class='list-group-item list-group-item-action'>${contact.name}</a>`;
			})

			text+=`</div>`

			$(".search-result").html(text);
			
			$(".search-result").show();
		})
		
	}
}

//first request to server- to create order
const paymentStart=()=>{
	console.log("Payment Started");
	let amount = $("#payment_field").val();
	console.log(amount);
	if(amount=='' || amount==null)
	{
		// alert("amount is required!!!");
		swal("Failed!!", "amount is required!!!", "error");
		return;
	}

	//using ajax to server to create order
$.ajax(
	{
		url:'/user/create_order',
		data:JSON.stringify({amount:amount, info: 'order_request'}),
		contentType:'application/json',
		type:'POST',
		dataType:'json',
		success:function(response){
			console.log(response);
			if(response.status=='created')
			{
				//open payment form
				let options = {
					"key": "rzp_test_UTT1lrHygK2ySp", // Enter the Key ID generated from the Dashboard
					"amount": response.amount, // Amount is in currency subunits. Default currency is INR. Hence, 50000 refers to 50000 paise
					"currency": "INR",
					"name": "Smart Contact Manager",
					"description": "Donation",
					"image": "https://images.app.goo.gl/k1khgskznCTZikwj8",
					"order_id": response.id, //This is a sample Order ID. Pass the `id` obtained in the response of Step 1
					"handler": function (response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						console.log('payment successful!!');

						updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,'paid');
						// alert("Congrats!! Payment Successful")
						
					},
					"prefill": {
						"name": "",
						"email": "",
						"contact": ""
					},
					"notes": {
						"address": "Each penny will count..."
					},
					"theme": {
						"color": "#3399cc"
					}
				};

				var rzp1 = new Razorpay(options);
				rzp1.on('payment.failed', function (response){
						console.log(response.error.code);
						console.log(response.error.description);
						console.log(response.error.source);
						console.log(response.error.step);
						console.log(response.error.reason);
						console.log(response.error.metadata.order_id);
						console.log(response.error.metadata.payment_id);
						// alert('Oops!! Payment Failed');
						swal("Failed!!", "Oops!! Payment Failed", "error");
				});

				rzp1.open();
			}
		},
		error:function(response){
			console.log(response);
			alert('something went wrong!!!');
		},
	}
)};

function updatePaymentOnServer(payment_id, order_id, order_status)
{
	$.ajax({
		url:'/user/update_order',
		data:JSON.stringify({payment_id:payment_id, order_id:order_id, order_status:order_status}),
		contentType:'application/json',
		type:'POST',
		dataType:'json',
		success:function(response){
			swal("Good job!", "Congrats!! Payment Successful", "success");
		},
		error:function(response){
			swal("Failed!!", "Your payment is successful but we did not capture it on our server. We will contact you as soon as possible", "error");
		},
	})
}
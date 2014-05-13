(function() {
	// retrieve time elapsed since the epoch
	timestamp = Math.ceil(new Date().getTime() / 1000).toString();
	
	// generate a pseudo-random nonce:
	// DO NOT USE THIS PLEASE 
	nonce = CryptoJS.MD5(timestamp + "abcdefghijklmnopqrstuvqxyz").toString();
	
	// identification
	oauth_consumer_key = "";
	oauth_token = "";
	
	// authentication - supposed to be kept secret ;)
	var oauth_consumer_secret = "";
	var oauth_token_secret = "";
	
	
	// [STEP 1] -> Create Parameter string: "metadata + necessary queryArgs"
	// piece together the essential parameters...
	
	var params = {
		"oauth_consumer_key": oauth_consumer_key, 
		"oauth_nonce": nonce,
		"oauth_signature_method": "HMAC-SHA1",
		"oauth_timestamp": timestamp,
		"oauth_token": oauth_token, 
		"oauth_version": "1.0" 
	};
	
	// GET -> refreshing the feed
	// POST -> posting a status update
	// (as far as this project is concerned)
	if (requestMethod == "GET") {
		params.since_id = since_id;
	} else if (requestMethod == "POST") {
		params.status = status; 
		params.in_reply_to_status_id = responseId;
		postData = ("in_reply_to_status_id=" + responseId + 
			"&status=" + encodeURIComponent(status).replace(/[-_.!~*'()]/g, function(x){return escape(x);})).replace(/%20/g,"+");
	}
	
	// sort the parameters alphabetically, %-encoding all key/value pairs,
	// this is done to ensure that the same hash is computed client and server side!
	var paramString = "";
	Object.keys(params).sort().forEach(function(element, index, array) {
		paramString += (element + "=" + encodeURIComponent(params[element]) + (index < array.length-1 ? "&" : ""));
	});
	paramString = paramString.replace(/[-_.!~*'()]/g, function(x){return escape(x);});
	
	// [STEP 2] -> Create Signature base string: "HTTP method + URL + parameters"
	// plaintext of what's about to get cryptographically signed...
	var baseString = requestMethod +
		"&" + encodeURIComponent(requestURL) + 
		"&" + encodeURIComponent(paramString);
			
	
	// [STEP 3] -> Create Signing key: "consumer_secret & OAuth_token_secret"
	// this is the key used to derive hmacsha1(baseString)...
	var signingKey = oauth_consumer_secret + "&" + oauth_token_secret;
	
	
	// [STEP 4] -> Calculate HMAC-SHA1 signature and base64 encode it
	// produces final result, hashed message authentication code... per spec.
	hash = encodeURIComponent(CryptoJS.HmacSHA1(baseString, signingKey).toString(CryptoJS.enc.Base64));
	
	
	// [STEP 5] -> Dump debug info and cURL command for testing
	if (verbose == "true") {
		println("\t[paramString] " + paramString);
		println("\t[baseString] " + baseString);
		println("\t[signingKey] " + signingKey)
		println("\t[HMAC] " + hash);
		
		var curl = "curl " + 
		"--request '" + requestMethod + 
		"' '" + requestURL + 
		"' --data '" + (requestMethod == "POST" ? postData : queryArgs) +
		"' --header '" + "Authorization: OAuth oauth_consumer_key=\"" + oauth_consumer_key + 
			"\", oauth_nonce=\"" + nonce + 
			"\", oauth_signature=\"" + hash + 
			"\", oauth_signature_method=\"" + "HMAC-SHA1" + 
			"\", oauth_timestamp=\"" + timestamp + 
			"\", oauth_token=\"" + oauth_token + 
			"\", oauth_version=\"" + "1.0" + 
			"\"" + 
		"' --verbose";
	
		// run this in your shell, if it returns a 200 OK, we're all set!
		println("cURL Command: " + curl);
	}
})();
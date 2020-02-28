<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Controlled Print Request</title>
    <link rel='stylesheet' href='https://cdnvault1.vod309.com/resources/dist/assets/styles/vault.main.2565ad9edeb0e833e95d562389f0da24.css'/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/css/select2.css" integrity="sha384-OHaMD69K68r4bj0APiEytPiO1eVdmz4FnEoFybuscWT1XgaY9+y2PBykmYsADpQ2" crossorigin="anonymous"> 
    <script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
   <!-- <script src="{{ contextPath }}/static/js/chosen.jquery.min.js"></script> -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/js/select2.full.min.js" integrity="sha384-zINbQLO66rPXZxY90sH8+rj07wZ4OUI/mXpU/veA2zjKWcHlY2VBCjeAqBBd8xUK" crossorigin="anonymous"></script>
</head>
<body>
<div id='IssuedCopyIntegrationWrapper'>


<div class="vv_box_main ">
  <div class="vofSection vv_vof_section" >
    <div class="detailContainer vv_box_section vv_details">
        <div class="vv_split_column vv_float_left">
          <div class="fieldRow vv_field_row vv_editable_row">
	        <div class="vv_right_value ">
		      <div class="inputContainer vv_input_field">
				<span selenium-value-name="documentName" class="vv_readonly_text  readonlyVofField">The Controlled Document request page cannot be displayed.</span>
		      </div>
	            </div>
          </div>
        <div class="fieldRow vv_field_row vv_editable_row">
            <div class="vv_right_value ">
	      <div class="inputContainer vv_input_field">
		<span selenium-value-name="errorReason" class="vv_readonly_text  readonlyVofField">Error Reason: ${msg}</span>
	      </div>
       	</div>
        </div>
        <div class="fieldRow vv_field_row vv_editable_row">
            <div class="vv_right_value ">
	      <div class="inputContainer vv_input_field">
		<span selenium-value-name="closureName" class="vv_readonly_text  readonlyVofField">Please close this window. Please contact: ${emailAddress}</span>
	      </div>
       	</div>
        </div>
	  </div>
	</div>
   <div class="dynamicSpacer"></div>
   </div>
  </div>
</div>
</div>
</body>
</html>
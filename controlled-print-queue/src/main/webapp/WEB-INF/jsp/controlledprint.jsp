<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="UTF-8" buffer="64kb" autoFlush="true"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<% response.addHeader("Content-Security-Policy", "frame-ancestors *.veevavault.com;"); %>
<title>Controlled Print Request</title>
    <link rel='stylesheet' href='https://cdnvault1.vod309.com/resources/dist/assets/styles/vault.main.2565ad9edeb0e833e95d562389f0da24.css'/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/css/select2.css" integrity="sha384-OHaMD69K68r4bj0APiEytPiO1eVdmz4FnEoFybuscWT1XgaY9+y2PBykmYsADpQ2" crossorigin="anonymous"> 
    <script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
   <!-- <script src="{{ contextPath }}/static/js/chosen.jquery.min.js"></script> -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/js/select2.full.min.js" integrity="sha384-zINbQLO66rPXZxY90sH8+rj07wZ4OUI/mXpU/veA2zjKWcHlY2VBCjeAqBBd8xUK" crossorigin="anonymous"></script>
</head>
<body>
<div id='IssuedCopyIntegrationWrapper'>
	<div class="vv_view_header">
		<h3 class="viewTitle vv_view_title">
			<span class="Seleniumn-View-Title viewTitle">Queue Controlled Print</span>
			<div class="buttonGroup vv_button_group">
				<button onclick='showHideHelp()' type="button" class="questionMarkHelpIcon autoBoundHelp vv_button vv_icon_button vv_documentation_link" data-message="This page allows you to request Issued Copies for production batch documentation. Fill in the correct batch number, audience and who the copy is requested for to continue." data-learnmoretext="Learn more" data-learnmoreurl="" data-vaulthelp="">
					<i class="fa fa-question-circle"></i>
				</button>
			</div>
		</h3>
	</div>

<div class="vv_box_main ">
  <div class="vofSection vv_vof_section" >
    <div class="detailContainer vv_box_section vv_details">
      <div style='border-bottom: 2px solid #f8972b; margin-bottom: 5px;'></div>
	
      <form:form method="POST" action="./result?domain=${domain}" modelAttribute="printrequest">
        <div class="vv_split_column vv_float_left">
          <!-- Requester Name -->
          <div class="fieldRow vv_field_row vv_editable_row  ">
	    <div class="vv_left_label ">
              <label class="vv_label " for="requesterName">
                <span selenium-name="requesterName">Requester Name : </span>
              </label>
            </div>
            <div class="vv_right_value ">
	      <div class="inputContainer vv_input_field">
		<span selenium-value-name="requesterName" class="vv_readonly_text  readonlyVofField">${requester}</span>
	      </div>
            </div>
          </div>
          <!-- Document Name -->
          <div class="fieldRow vv_field_row vv_editable_row  ">
	    <div class="vv_left_label ">
              <label class="vv_label " for="documentName">
                <span selenium-name="version">Controlled Print : </span>
              </label>
            </div>
            <div class="vv_right_value ">
	      <div class="inputContainer vv_input_field">
		<span selenium-value-name="documentName" class="vv_readonly_text  readonlyVofField">${documentName}</span>
	      </div>
       	</div>
        </div>
	
		<!-- Number Of Copies Dropdown -->
          <div class="fieldRow vv_field_row vv_editable_row  ">
            <div class="vv_left_label ">
              <label class="vv_label " for="noOfCopies">
                <span selenium-name="noOfCopies">Number of Copies : </span>
              </label>
            </div>
            <div class="vv_right_value" style="width:10%">
				<form:select id="noOfCopies" name="noOfCopies" path="NumberOfCopies" style="width: 100%; background-color:#ffc;">
					<c:forEach begin="1" end="${maxCopies}" var="i">
						<option value="${i}">${i}</option>
					</c:forEach>
				</form:select>
            </div>
          </div>
          <!-- Reason for Justification Dropdown -->
          <div class="fieldRow vv_field_row vv_editable_row  ">
            <div class="vv_left_label ">
              <label class="vv_label " for="reason">
                <span selenium-name="reason">Reason/Justification : </span>
              </label>
            </div>
            <div class="vv_right_value">
				<form:select id="reason" name="reason" path="PrintReason" style="width: 100%; background-color:#ffc;">			
					<form:options items="${printReasonSet}" />
				</form:select>
            </div>
          </div>
          
          <!-- Printers -->
          <div class="fieldRow vv_field_row vv_editable_row  ">
            <div class="vv_left_label ">
              <label class="vv_label " for="printerName">
                <span selenium-name="printerName">Printer : </span>
              </label>
            </div>
            <div class="vv_right_value ">
				<form:select id="printerName" name="printerName" path="PrinterId" style="width: 100%; background-color:#ffc;">
					<form:options items="${printers}" itemValue="Id" itemLabel="Name" />
				</form:select>
            </div>
          </div>
          
          <!-- Requested for Ends -->
          
          <div class="headerButtons vv_button_group" style="margin-left: 40%; width: 60%;float: left; max-width: 323px;">
            <input class='saveAction vv_button vv_button_primary' type='submit' value='Request' style='background-color:rgb(10, 136, 197); margin: 25px 0 0 0;' />
          </div>          	  
	  <script>
	  </script>
	    
	  </div>
	</div>
	
	<form:input type="hidden" id="documentId" name="documentId" path="DocumentId" value="${printerRequest.documentId}"/>
	<form:input type="hidden" id="majorVersion" name="majorVersion" path="majorVersion" value="${majorVersion}"/>
	<form:input type="hidden" id="minorVersion" name="minorVersion" path="minorVersion" value="${minorVersion}"/>
	<form:input type="hidden" id="userId" name="userId" path="UserId" value="${printerRequest.userId}"/>
  </form:form>

      <div class="dynamicSpacer"></div>
      </div>
  </div>
</div>
</div>
<div class="vaultHelpTooltip ui-widget ui-widget-content ui-corner-all vv_guidance_panel" style="top: 30px; left: 188px; display:none;">
	<div class="vv_talk_bubble">
		<div class="vv_guidance_tail guidanceUp"></div>
			<div class="vHTMessage">This page allows you to request Issued Copies for production batch documentation. Fill in the correct batch number, audience and who the copy is requested for to continue.</div>
		<div class="vv_guidance_tail_flipped guidanceDown" style="display:none"></div>
	</div>
</div>
</body>
</html>
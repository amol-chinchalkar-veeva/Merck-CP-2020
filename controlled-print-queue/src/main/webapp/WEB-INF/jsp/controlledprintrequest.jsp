<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="UTF-8" buffer="64kb" autoFlush="true"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Controlled Print Request</title>
<script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.6-rc.0/js/select2.full.js" charset="UTF-8"></script>
<script src="https://code.jquery.com/ui/1.11.0/jquery-ui.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.6-rc.0/css/select2.min.css" />
<link rel='stylesheet' href='https://cdnvault1.vod309.com/resources/dist/assets/styles/vault.main.2565ad9edeb0e833e95d562389f0da24.css'/>

<style type="text/css">

.select2 .mandatory, .mandatory {
	background-color:#ffc;
}
 <%-- render JSON to create a controlled print copy fields on the form --%> 
</style>

<script type="text/javascript">
var sessionId = "";
var userId = "";
var vaultDomain = "";
var copyType = "";
var docName = "";
var docId;
var jsonData;

$(document).ready(function() {
	
	  jsonData = JSON.parse('${data}');
	  var controls = jsonData.Controls;
	  sessionId = '${sessionId}';
	  userId = '${userId}';
	  vaultDomain = '${vaultDomain}';
	  copyType = '${copyType}';
	  docName = '${docName}';
	  docId = '${docId}';
	  
	  for(i in controls) {
		  addNewControl(controls[i]);
	  }
	});

	function addNewControl(control) {
		switch (control.Type) {
			case "Text":
				renderTextBox(control);
				break;
			case "TextArea":
				renderTextArea(control);
				break;		
			case "ObjectDropdown":
			case "Picklist":
				renderObjectDropdown(control);
				break;
			case "LazyDropdown":
				renderLazyDropdown(control);
				break;
			case "UsersDropdown":
				renderUsersDropdown(control);
				break;
			case "VQLFilteredDropdown":
				renderVQLFilteredDropdown(control);
				break;
			case "Numeric":
				renderNumericTextBox(control);
				break;
			case "Boolean":
				renderBooleanDropdown(control);
				break;
			case "Date":
				renderDatePicker(control);
				break;
			default:
				break;
		}
	}
	
	
	function numberValidate(value, min, max) {

		var nonDigit = /\D/;
		if(value.match(nonDigit)) {
			return false;
		} else if (parseInt(value) < min || parseInt(value) > max) {
			return false;
		}
		
		return true;
	}
	
	function renderNumericTextBox(control) {
		
		if(control.MaxValue != null) {
			var $textBox = $("<input type='Text' />").attr('id', control.Name).addClass("controlledcopy");
			$textBox.val(control.MinValue)
			//$textBox.attr('title': control.Tooltip);
			if(control.Mandatory) {
				$textBox.addClass("mandatory");
			}
			
			addControlToPage(control.Label, $textBox);
			$errorlabel = $("<label />").attr('id', "lbl_error_" + control.Name).css('color', 'red');
			$errorlabel.text("Invalid Number. Min: " + control.MinValue + " Max: " + control.MaxValue);
			$textBox.closest('div').append($errorlabel);
			$("#lbl_error_" + control.Name).hide();
			
			$textBox.keyup(function(){
				
				var bValid = numberValidate($textBox.val(), control.MinValue, control.MaxValue);
				
				if(bValid || $textBox.val().length == 0) {
					
					if(control.Mandatory) {
						$textBox.css("background-color", "#ffc");
					} else {
						$textBox.css("background-color", "white");
					}
					$textBox.css("border-color", "#ccc");
					$("#lbl_error_" + control.Name).hide();
					
					
				} else {
					$textBox.css("border-color", "red");
					$("#lbl_error_" + control.Name).show();
				}
			});
		}
	}
		
	function renderObjectDropdown(control) {

		if(control.Items != null) {
			var $dropdown = $('<select><option selected=""></option></select>').attr('id', control.Name);
				//$dropdown.attr('title', control.Tooltip);
			$.each(control.Items, function(key, value) {
				$dropdown.append(new Option(value.Name, value.Id))
			});
		
			var cssClass = '';
			if(control.Mandatory == true) {
				cssClass = 'mandatory';
			}
			
			addControlToPage(control.Label, $dropdown);
			$dropdown.select2({containerCssClass: cssClass, allowClear:!control.Mandatory, placeholder:""});
		}
	}
	
	function renderLazyDropdown(control) {
		var $dropdown = $('<select />').attr('id', control.Name);
		
		addControlToPage(control.Label, $dropdown);
		
		var cssClass = '';
		if(control.Mandatory == true) {
			cssClass = 'mandatory';
		} else {
			$dropdown.prepend('<option selected=""></option>');
		}
		
		// Dropdown must be on the form before this command
		$dropdown.select2({
		containerCssClass: cssClass,
		allowClear: !control.Mandatory,
		placeholder:"",
		   ajax: {
					url: 'lazydropdown',
					dataType: 'json',
					delay:1000,
					data: function(params) {
						var query = {
								field: control.Name,
								copytype: copyType,
								vaultdomain:vaultDomain,
								term: params.term,
								sessionId: sessionId,
								userId: userId
						}
						
						return query;
					}
	  			}
	 		});
	}
	
	function renderVQLFilteredDropdown(control) {
		var $dropdown = $('<select />').attr('id', control.Name);
		addControlToPage(control.Label, $dropdown);

		var cssClass = '';
		if(control.Mandatory == true) {
			cssClass = 'mandatory';
		} else {
			$dropdown.prepend('<option selected=""></option>');
		}
		
		// Dropdown must be on the form before this command
		$dropdown.select2({
		   containerCssClass: cssClass,
		   allowClear: !control.Mandatory,
		   placeholder:"",
		   ajax: {
					url: 'vqldropdown',
					dataType: 'json',
					delay:1000,
					data: function(params) {
						var query = {
								field: control.Name,
								copytype: copyType,
								vaultdomain:vaultDomain,
								docName : docName,
								docId : docId,
								term: params.term,
								sessionId: sessionId,
								userId: userId
						}
						
						return query;
					}
	  			}
	 		});
	}
	
	function renderUsersDropdown(control) {
		var $dropdown = $('<select />').attr('id', control.Name);
			//$dropdown.attr('title', control.Tooltip);
		addControlToPage(control.Label, $dropdown);
		var cssClass = '';
		if(control.Mandatory == true) {
			cssClass = 'mandatory';
		} else {
			$dropdown.prepend('<option selected=""></option>');
		}
		
		// Dropdown must be on the form before this command
		$dropdown.select2({
		   containerCssClass: cssClass,
		   allowClear: !control.Mandatory,
		   placeholder:"",
		   ajax: {
					url: 'usersdropdown',
					dataType: 'json',
					delay:2000,
					data: function(params) {
						var query = {
								vaultdomain:vaultDomain,
								term: params.term,
								sessionId: sessionId,
								userId: userId
						}
						
						return query;
					}
	  			}
	 		});
	}
	
	function renderTextArea(control) {
		var $textArea = $("<textarea rows='3' cols='50' />").attr('id', control.Name);
		$textArea.attr('maxLength', control.Length);
		$textArea.css({"max-width" : "323px", "width" : "100%" });
		//$textArea.attr('title', control.Tooltip);
		if(control.Mandatory) {
			$textArea.css("background-color", "#ffc");
		}
		
		addControlToPage(control.Label, $textArea);
	}
	
	function renderTextBox(control) {
		var $textBox = $("<input type='Text'/>").attr('id', control.Name);
		$textBox.attr('maxLength', control.Length);
		//$textBox.attr('title', control.Tooltip);
		
		if(control.Mandatory) {
			$textArea.css("background-color", "#ffc");
		}
		
		addControlToPage(control.Label, $textBox);
	}
	
		
	function renderBooleanDropdown(control) {
		var $dropdown = $("<select  />").attr('id', control.Name);
		$dropdown.append(new Option("", ""));
		$dropdown.append(new Option("Yes", true));
		$dropdown.append(new Option("No", false));
		//$dropdown.attr('title', control.Tooltip);
		if(control.Mandatory) {
			$dropdown.css("background-color", "#ffc");
		}
		
		addControlToPage(control.Label, $dropdown);
	}
	
	function renderDatePicker(control) {
		var $datePicker = $("<input type='text' readonly='readonly' style='background-color: #FFFFFF'  />").attr('id', control.Name);
		$datePicker.datepicker({ dateFormat: 'yy-mm-dd'});
		$datePicker.attr('title', control.Tooltip);
		if(control.Mandatory) {
			$datePicker.css("background-color", "#ffc");
		}
		
		addControlToPage(control.Label, $datePicker);
	}
	
	function addControlToPage(strLabelText, jqueryControl) {
		$label = $('<label />').addClass("vv_label").text(strLabelText);
		$labelDiv = $('<div />').addClass("vv_left_label").append($label);
		$controlDiv = $('<div />').addClass("vv_right_value").append(jqueryControl);
		$rowDiv = $('<div />').addClass("vv_editable_row").append($labelDiv).append($controlDiv);
		$('#FormDiv').append($rowDiv);
	}
	
	function continueClick() {
		var bIsValid = true;
		var controls = jsonData.Controls;
		var copyData = {};
		var sErrorText = "";
		
		for(var i in controls) {
			var controlName = controls[i].Name;
			if(controls[i].Mandatory) {
				var controlValue = $("#" + controlName).val();
				if(controlValue == null || controlValue == '') {
					bIsValid = false;
					sErrorText = "Please enter the required information.";
					break;
				}
			}
			
			if(controls[i].Type == 'Numeric' && controlValue != '') {
				bIsValid = numberValidate($("#" + controlName).val(), controls[i].MinValue, controls[i].MaxValue);
				if(!bIsValid) {
					sErrorText = "Please enter valid numeric values.";
					break;
				}
			}
			
			copyData[controlName] = $("#" + controlName).val();
		}
		
		if(bIsValid) {
			$('#ErrorText').empty();
			$("#controlText").val(JSON.stringify(copyData));
			$("#Continue").attr("disabled", true);
			$("#CCForm").submit();
		} else {
			$('#ErrorText').html(sErrorText);
		}
	}
	
</script>
 <%--POST request  --%> 
</head>
<body>
 <form:form id="CCForm" method="POST" action="./result?vaultDomain=${vaultDomain}" modelAttribute="copyrequest">
	<div class="vv_view_header">
		<h3 class="viewTitle vv_view_title">Queue Controlled Print</h3>
	</div>
	<div style='border-bottom: 2px solid #f8972b; margin-bottom: 5px;'></div>
	<div class="vv_box_section">
		<div id="FormDiv" class="vv_split_column">
			<div class="vv_editable_row">
				<div class="vv_left_label">
					<label class="vv_label">Document Name</label>
				</div>
				<div class="vv_right_value">
					<label>${friendlyDocName}</label>
				</div>
			</div>
		</div>
	</div>
	<div style="margin-left: 20%; margin-top:15px; width: 60%; max-width: 323px">
		<input type="button" id="Continue" value="Print" class="vv_button vv_button_primary" style="float: right;" onclick="continueClick()" />
	</div>
	<div style="margin-left: 20%; width: 60%; max-width: 323px">
		<label id="ErrorText" style="color:red;"></label>
	</div>
	<form:input type="hidden" id="controlText" name="controlText" path="controlText" />
	<form:input type="hidden" id="documentId" name="documentId" path="documentId" Value="${docId}" />
	<form:input type="hidden" id="documentName" name="documentName" path="documentName" Value="${docName}" />
	<form:input type="hidden" id="majorVersion" name="majorVersion" path="majorVersion" Value="${majorVersion}" />
	<form:input type="hidden" id="minorVersion" name="minorVersion" path="minorVersion" Value="${minorVersion}" />
	<form:input type="hidden" id="actionLabel" name="actionLabel" path="actionLabel" Value="${actionLabel}" />
	<form:input type="hidden" id="userId" name="userId" path="userId" Value="${userId}" />
 </form:form>
</body>
</html>
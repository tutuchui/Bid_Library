function editSection(){
    var content = $("#content").html();
    var content_title = $("#content_title").val();
    var version = $("#version").val();
    var section_name = $("#section_name").val();

    $.ajax({
        url:"edit_upload.do",
        data:{"content_title":content_title,"section_name":section_name,"version":version,"content_detail":content},
        type:"post",
        dataType:"json",
        contentType:"application/x-www-form-urlencoded",
        success:function (data) {
            if(data[0].info == "true"){
                $(location).attr('href','success_upload');
            }
            else{
                $(location).attr('href','error');
            }
        },
        error:function(XMLHttpRequest, textStatus, errorThrown) {
            alert(XMLHttpRequest.status);
        }
    })
}


package atos.controller;

import atos.admain.*;
import atos.dao.SolutionDao;
import atos.dao.UserDao;
import atos.exceptions.NoSearchResultException;
import atos.wordExport.RichHtmlHandler;
import atos.wordExport.WordGeneratorWithFreemarker;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class UserController {
    @Resource
    private UserDao userDao;
    @Resource
    private SolutionDao solutionDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setSolutionDao(SolutionDao solutionDao) {
        this.solutionDao = solutionDao;
    }

    public SolutionDao getSolutionDao() {
        return solutionDao;
    }


    /**
     * Test if the user has log in the system, if not, then redirected to the log in page.
     * @param request
     * @param model
     * @return
     */
    private String testIfLogIn(HttpServletRequest request,ModelMap model)
    {
        if(request.getSession().getAttribute("loginstaff")!=null) {
            UserVO loginstaff = (UserVO) request.getSession().getAttribute("loginstaff");
            model.addAttribute("name",loginstaff.getName());
            if(loginstaff.getRole()){
                model.addAttribute("role","Admin");
            }
            else{
                model.addAttribute("role","User");
            }
            return "true";

        }else{
            return "unlogin";
        }

    }

    /**
     * @param request
     * @param model
     * @return direct to the staff_search page.
     */
    @RequestMapping(value = "/staff_search", method = GET)
    public String staffSearchPage(HttpServletRequest request, ModelMap model) {
        if(!testIfLogIn(request,model).equals("true"))
        {
            return testIfLogIn(request,model);
        }
        List<SolutionVO> contentList = solutionDao.selectAll();
        if (!contentList.isEmpty()) {
            model.addAttribute("content_list", contentList);
        } else {
            throw new NoSearchResultException("No Available Content", "Sorry, there is no content now");
        }
        for (int i = 0; i < contentList.size(); i++) {
            String content_title = contentList.get(i).getContent_title();
            String type = contentList.get(i).getIsExternal();
            List<SectionVO> sectionList = solutionDao.selectInUseSection(content_title,type);
            model.addAttribute("section" + i, sectionList);
        }
        List<TemplateVO> allTemplates = solutionDao.selectAllTemplates();
        model.addAttribute("allTemplates",allTemplates);
        return "staff_search";

    }

    /**
     * let the user can search content or section with keywords
     * @param request
     * @param model
     * @return
     */
    @RequestMapping(value = "/search.do", method = POST)
    public String staffSearchResultPage(HttpServletRequest request, ModelMap model) {
        if(!testIfLogIn(request,model).equals("true"))
        {
            return testIfLogIn(request,model);
        }
        List<TemplateVO> allTemplates = solutionDao.selectAllTemplates();
        model.addAttribute("allTemplates",allTemplates);

        String searchArea = request.getParameter("tag");
        String keyword = request.getParameter("keyword");
        List<SolutionVO> resultContentList = new ArrayList<SolutionVO>();
        List<SectionVO> resultSectionList = new ArrayList<SectionVO>();
        List<SectionVO> resultSectionList2 = new ArrayList<SectionVO>();
        List<SectionVO> finalSectionList = new ArrayList<SectionVO>();
        switch (searchArea) {
            case "Default":
                resultContentList = solutionDao.selectByDefault(keyword);
                resultSectionList = solutionDao.searchInUseSectionByName(keyword);
                resultSectionList2 = solutionDao.searchInUseSectionByDetails(keyword);
                finalSectionList.addAll(resultSectionList);
                finalSectionList.addAll(resultSectionList2);
                for (SectionVO resultS : finalSectionList) {
                    int isExist = 0;
                    //check the search content if already exists in the result list.
                    for (SolutionVO resultC : resultContentList) {
                        if (resultS.getTitle().equals(resultC.getContent_title())&&resultS.getIsExternal().equals(resultC.getIsExternal())) {
                            isExist = 1;
                            break;
                        }
                    }
                    if (isExist == 0) {
                        SolutionVO newContent = solutionDao.selectContentByTitle(resultS.getTitle(),resultS.getIsExternal());
                        resultContentList.add(newContent);
                    }
                }
                break;
            case "Content Title":
                resultContentList = solutionDao.searchByTitle(keyword);
                break;
            case "Author":
                resultContentList = solutionDao.searchByAuthor(keyword);
                break;
            case "Type":
                resultContentList = solutionDao.searchByType(keyword);
                break;
            case "Customer":
                resultContentList = solutionDao.searchByCustomer(keyword);
                break;
            case "Section Name":
                resultSectionList = solutionDao.searchInUseSectionByName(keyword);
                for (SectionVO resultS : resultSectionList) {

                    SolutionVO newContent = solutionDao.selectContentByTitle(resultS.getTitle(),resultS.getIsExternal());
                    resultContentList.add(newContent);
                }

        }

        if (!resultContentList.isEmpty()) {
            model.addAttribute("content_list", resultContentList);
            for (int i = 0; i < resultContentList.size(); i++) {
                String content_title = resultContentList.get(i).getContent_title();
                String type = resultContentList.get(i).getIsExternal();
                List<SectionVO> sectionList = solutionDao.selectInUseSection(content_title,type);
                model.addAttribute("section" + i, sectionList);
            }
        } else {
            throw new NoSearchResultException("No Result", "Sorry, there is no matched result, please try another keyword");
        }
        model.addAttribute("keyword",keyword);
        return "staff_search";
    }


    @ExceptionHandler(NoSearchResultException.class)
    public ModelAndView handleAdministerExceptionException(HttpServletRequest request,NoSearchResultException ex) {
        ModelAndView modelAndView = new ModelAndView("staff_error");
        if(request.getSession().getAttribute("loginstaff")!=null) {
            UserVO loginstaff = (UserVO) request.getSession().getAttribute("loginstaff");
            modelAndView.addObject("name",loginstaff.getName());
            if(loginstaff.getRole()){
                modelAndView.addObject("role","Admin");
            }
            else{
                modelAndView.addObject("role","User");
            }

        }else{
            return new ModelAndView("unlogin");
        }
        modelAndView.addObject("errCode", ex.getErrCode());
        modelAndView.addObject("errMsg", ex.getErrMsg());
        return modelAndView;
    }

    /**
     * display the selected section detail to users
     * @param request
     * @param model
     * @return
     */
    @RequestMapping(value = "userViewDetail.do", method = GET)
    public String userViewDetail(HttpServletRequest request, ModelMap model) {
        if(!testIfLogIn(request,model).equals("true"))
        {
            return testIfLogIn(request,model);
        }
        String content_title = request.getParameter("content_title");
        String section_name = request.getParameter("section_name");
        String str_version = request.getParameter("version");
        String type = request.getParameter("isExternal");
        if (content_title == null || section_name == null || str_version == null) {
            return "staff_search";
        }
        int version = Integer.parseInt(str_version);
        SectionVO requestSection = solutionDao.selectSectionByName(content_title, section_name, version,type);
        String details = requestSection.getSection_details().replaceAll("\n", "</p><p>");
        model.addAttribute("content_title", content_title);
        model.addAttribute("version", version);
        model.addAttribute("section_name", section_name);
        model.addAttribute("content", details);
        return "UserDetail";
    }

    /**
     * get the section detail of  selected sections, put it into right position of the selected template.
     * @param request
     * @param model
     * @param list selected content titles
     * @param list2 selected content types
     * @param selected_template the name of selected template
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "export_word", method = POST)
    @ResponseBody
    public List<Userjson> exportWord(HttpServletRequest request, ModelMap model, @RequestParam(required = false, value = "list[]") List<String> list, @RequestParam(required = false, value = "list2[]") List<String> list2, String selected_template) throws IOException {
        UserVO loginstaff = (UserVO) request.getSession().getAttribute("loginstaff");
        String user = loginstaff.getName();
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String download_time =  dateFormat.format(now);
        List<SolutionVO> targetContents = new ArrayList<SolutionVO>();
        List<Userjson> response = new ArrayList<Userjson>();
        Userjson jsonInfo = new Userjson();
        response.add(jsonInfo);
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (int i = 0;i<list.size();i++) {
            SolutionVO temp = solutionDao.selectContentByTitle(list.get(i),list2.get(i));
            targetContents.add(temp);
        }
        for (SolutionVO temp : targetContents) {
            List<SectionVO> sectionList = solutionDao.selectInUseSection(temp.getContent_title(),temp.getIsExternal());
            map.put("CustomerName",temp.getCustomer());
            for (SectionVO sectionTemp : sectionList) {
                String data = sectionTemp.getSection_details();
                String orginalData = "";
                String section_name = sectionTemp.getSection_name().replaceAll(" ", "");

                if (map.containsKey(section_name)) {
                    orginalData = (String) map.get(section_name);
                    data = orginalData + data;
                }
                RichHtmlHandler handler = new RichHtmlHandler(data);
                TemplateVO template = solutionDao.selectTemplateByName(selected_template);
                handler.setDocSrcLocationPrex(template.getDoc_src_prefix_location());
                handler.setNextPartId(template.getNext_part_id());
                handler.setDocSrcParent(template.getDoc_src_parent());
                handler.setShapeidPrex("_x56fe_x7247_x0020");
                handler.setSpidPrex("_x0000_i");
                handler.setTypeid("#_x0000_t75");
                handler.handledHtml(true,request);
                String bodyBlock = handler.getHandledDocBodyBlock();
                String handledBase64Block = "";
                if (handler.getDocBase64BlockResults() != null
                        && handler.getDocBase64BlockResults().size() > 0) {
                    for (String item : handler.getDocBase64BlockResults()) {
                        handledBase64Block += item + "\n";
                    }
                }
                String old_handldBase64Block = "";
                if (map.containsKey("imagesBase64String")) {
                    old_handldBase64Block = (String) map.get("imagesBase64String");
                    handledBase64Block = old_handldBase64Block + handledBase64Block;
                }
                map.put("imagesBase64String", handledBase64Block);
                String xmlimaHref = "";
                if (handler.getXmlImgRefs() != null && handler.getXmlImgRefs().size() > 0) {
                    for (String item : handler.getXmlImgRefs()) {
                        xmlimaHref += item + "\n";
                    }
                }
                String old_xmlimaHref = "";
                if (map.containsKey("imagesXmlHrefString")) {
                    old_xmlimaHref = (String) map.get("imagesXmlHrefString");
                    xmlimaHref = old_xmlimaHref + xmlimaHref;
                }
                map.put("imagesXmlHrefString", xmlimaHref);
                map.put(section_name, bodyBlock);
            }
        }
        String filePath = request.getSession().getServletContext().getRealPath("WEB-INF/view/download"+File.separator);
        String fileName = selected_template+"_Generate.doc";
        File f = new File(filePath+fileName);
        int i = 1;
        String new_file_name = fileName;
        while(f.exists()){
            new_file_name = selected_template + "_Generate("+i+").doc";
            f = new File(filePath+new_file_name);
            i++;
        }
        StringBuilder selected_content = new StringBuilder();
        for(int x =0;x<list.size();x++){
            selected_content.append(list.get(x)).append("-").append(list2.get(x)).append("/");
        }
        request.getSession().setAttribute("download_file",new_file_name);
        OutputStream out;
        try {
            out = new FileOutputStream(f);
            WordGeneratorWithFreemarker.createDoc(map,selected_template+".ftl", out);
            solutionDao.storeDownloadLog(new_file_name, selected_content.toString(),selected_template,download_time,user);
            jsonInfo.setInfo("true");
            response.add(jsonInfo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            jsonInfo.setInfo(e.toString());
            response.add(jsonInfo);
        } catch (MalformedTemplateNameException e) {
            e.printStackTrace();
            jsonInfo.setInfo(e.toString());
            response.add(jsonInfo);

        } catch (ParseException e) {
            e.printStackTrace();
            jsonInfo.setInfo(e.toString());
            response.add(jsonInfo);
        } catch (IOException e) {
            e.printStackTrace();
            jsonInfo.setInfo(e.toString());
            response.add(jsonInfo);
        } catch (Exception e) {
            e.printStackTrace();
            jsonInfo.setInfo(e.toString());
            response.add(jsonInfo);
        }
        return response;

    }

    @RequestMapping(value = "success_generate", method = GET)
    public ResponseEntity<byte[]> showSuccessGenerate(HttpServletRequest request, ModelMap model) {
        String filePath = request.getSession().getServletContext().getRealPath("WEB-INF/view/download"+File.separator);
        String fileName = (String) request.getSession().getAttribute("download_file");
        request.getSession().removeAttribute("download_file");
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setContentDispositionFormData("myfile", fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            File doc = new File(filePath, fileName);
            return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(doc),headers,HttpStatus.CREATED);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;

    }


    @RequestMapping(value = "generate_error",method = GET)
    public String showGenerateError(HttpServletRequest request,ModelMap model){
        return "generate_error";
    }

    @RequestMapping(value="unlogin",method = GET)
    public String jumpToLogin(HttpServletRequest request,ModelMap model){
        return "unlogin";
    }

    @RequestMapping(value="unAdmin",method = GET)
    public String unAdmin(HttpServletRequest request,ModelMap model){
        return "unAdmin";
    }
}
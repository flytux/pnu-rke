package egovframework.com.utl.fcc.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.logging.log4j.LogManager;

import egovframework.com.cmm.EgovWebUtil;

/**
 * @Class Name  : EgovFormBasedFileUtil.java
 * @Description : Form-based File Upload 유틸리티
 * @Modification Information
 *
 *     수정일         수정자                   수정내용
 *     -------          --------        ---------------------------
 *   2009.08.26       한성곤                  최초 생성
 *
 * @author 공통컴포넌트 개발팀 한성곤
 * @since 2009.08.26
 * @version 1.0
 * @see
 */
public class EgovFormBasedFileUtil {
    /** Buffer size */
    public static final int BUFFER_SIZE = 8192;

    public static final String SEPERATOR = File.separator;

    /** 
     * 오늘 날짜 문자열 취득.
     * ex) 20090101
     * @return
     */
    public static String getTodayString() {
	SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

	return format.format(new Date());
    }

    /**
     * 물리적 파일명 생성.
     * @return
     */
    public static String getPhysicalFileName() {
	return EgovFormBasedUUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

    /**
     * 파일명 변환.
     * @param filename String
     * @return
     * @throws Exception
     */
    protected static String convert(String filename) throws Exception {
	//return java.net.URLEncoder.encode(filename, "utf-8");
	return filename;
    }

    /**
     * Stream으로부터 파일을 저장함.
     * @param is InputStream
     * @param file File
     * @throws IOException
     */
    public static long saveFile(InputStream is, File file) throws IOException {
	// 디렉토리 생성
	if (! file.getParentFile().exists()) {
	    file.getParentFile().mkdirs();
	}

	OutputStream os = null;
	long size = 0L;

	try {
		os = new FileOutputStream(file);

	    int bytesRead = 0;
	    byte[] buffer = new byte[BUFFER_SIZE];

	    while (true) {
	        bytesRead = is.read(buffer, 0, BUFFER_SIZE);
	        if(bytesRead  == -1) {
	            break;
	        }
			size += bytesRead;
			os.write(buffer, 0, bytesRead);
	    }
	    
	    os.close();
	/** 시큐어 코딩 : 일반적 예외처리 **/
	} catch (FileNotFoundException e) {
		LogManager.getLogger(EgovFormBasedFileUtil.class).debug("IGNORED: " + e.getMessage());
	} catch (IOException e) {
		LogManager.getLogger(EgovFormBasedFileUtil.class).debug("IGNORED: " + e.getMessage());
	} catch (Exception e) {
		LogManager.getLogger(EgovFormBasedFileUtil.class).debug("IGNORED: " + e.getMessage());
	} finally {
	    if (os != null) {
	    	try{
	    		os.close();
			} catch (IOException ignore) {
				LogManager.getLogger(EgovFormBasedFileUtil.class).debug("IGNORE: " + ignore);
			}
	    }
	}

	return size;
    }

    /**
     * 파일을 Upload 처리한다.
     *
     * @param request
     * @param where
     * @param maxFileSize
     * @return
     * @throws Exception
     */
    public static List<EgovFormBasedFileVo> uploadFiles(HttpServletRequest request, String where, long maxFileSize) throws Exception {
	List<EgovFormBasedFileVo> list = new ArrayList<EgovFormBasedFileVo>();

	// Check that we have a file upload request
	boolean isMultipart = ServletFileUpload.isMultipartContent(request);

	if (isMultipart) {
	    // Create a new file upload handler
	    ServletFileUpload upload = new ServletFileUpload();
	    upload.setFileSizeMax(maxFileSize);	// SizeLimitExceededException

	    // Parse the request
	    FileItemIterator iter = upload.getItemIterator(request);
	    while (iter.hasNext()) {
	        FileItemStream item = iter.next();
	        String name = item.getFieldName();
	        InputStream stream = item.openStream();
	        if (item.isFormField()) {
	            //log.debug("Form field '" + name + "' with value '" + Streams.asString(stream) + "' detected.");
	            LogManager.getLogger(EgovFormBasedFileUtil.class).info("Form field '" + name + "' with value '" + Streams.asString(stream) + "' detected.");
	        } else {
	            //log.debug("File field '" + name + "' with file name '" + item.getName() + "' detected.");
	            LogManager.getLogger(EgovFormBasedFileUtil.class).info("File field '" + name + "' with file name '" + item.getName() + "' detected.");

	            if ("".equals(item.getName())) {
	        	continue;
	            }

	            // Process the input stream
	            EgovFormBasedFileVo vo = new EgovFormBasedFileVo();

	            String tmp = item.getName();

	            if (tmp.lastIndexOf("\\") >= 0) {
	        	tmp = tmp.substring(tmp.lastIndexOf("\\") + 1);
	            }

	            vo.setFileName(tmp);
	            vo.setContentType(item.getContentType());
	            vo.setServerSubPath(getTodayString());
	            vo.setPhysicalName(getPhysicalFileName());

	            if (tmp.lastIndexOf(".") >= 0) {
	        	 vo.setPhysicalName(vo.getPhysicalName() + tmp.substring(tmp.lastIndexOf(".")));
	            }

	            long size = saveFile(stream, new File(EgovWebUtil.filePathBlackList(where) + SEPERATOR + vo.getServerSubPath() + SEPERATOR + vo.getPhysicalName()));

	            vo.setSize(size);

	            list.add(vo);
	        }
	    }
	} else {
	    throw new IOException("form's 'enctype' attribute have to be 'multipart/form-data'");
	}

	return list;
    }

   

    
}
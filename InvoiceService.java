package com.gpch.pdfgenerator.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.gpch.pdfgenerator.model.FinalInvoiceModel;
import com.gpch.pdfgenerator.model.InvoiceJSONModel;
import com.gpch.pdfgenerator.model.ItemModel;

@Service
public class InvoiceService {

	@Value("${invoice.writeJson.filepath}")
	String writefilePath;
	
    @Autowired
    private ConvertService convertService;
    
    public List<FinalInvoiceModel> getInvoices(){
    	List<InvoiceJSONModel> allInvoices= new ArrayList<>();
    	
    	try {
			allInvoices=convertService.convert();
			
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	   	

    List<FinalInvoiceModel> listWithoutDuplicates =  createFinalInvoice(allInvoices).stream().distinct().collect(Collectors.toList());
    	
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    for(FinalInvoiceModel toWrite : listWithoutDuplicates)
    {
    
    	Path path = Paths.get(writefilePath+ toWrite.getInvoiceNo() +".json");
    	if(!Files.isRegularFile(path))
    	{
    	 try {
			String json = ow.writeValueAsString(toWrite);
			Files.write(path, json.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	}
    	else
    	{
    		System.out.println("File exists Skipping " + path.toString());
    	}
    }
   
		
    return listWithoutDuplicates;
    	
    	
    }
    
    public List<FinalInvoiceModel> createFinalInvoice(List<InvoiceJSONModel> invoices)
    {
    	List<FinalInvoiceModel> allInvoices= new ArrayList<>();
    	try {
    	
    	for (InvoiceJSONModel invoiceItem : invoices)
    	{
    		FinalInvoiceModel invoice= new FinalInvoiceModel();
    		invoice.setInvoiceNo(invoiceItem.getInvoiceno());
    		DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
    		Date date = (Date)formatter.parse(invoiceItem.getInvoicedt());
    		
    		SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy");  
    	    String strDate= formatter2.format(date);  
    		
    		invoice.setDate(strDate);
    		invoice.setCname(invoiceItem.getPrtyname());  
    		invoice.setCaddress(invoiceItem.getAddress());
    		invoice.setCgstno(invoiceItem.getpVatNo());   
    		invoice.setPaymentMode(invoiceItem.getPmterms());
    		invoice.setPhone(invoiceItem.getPhone());
    		final List<ItemModel> invItems= new ArrayList<>();
    		Double sgst = Double.valueOf(0) ;
    		Double cgst = Double.valueOf(0) ;
    		Double total = Double.valueOf(0) ;
    		Double totNop = Double.valueOf(0) ;
    		Double totVBP = Double.valueOf(0) ;
    		Double totalqty = Double.valueOf(0) ;
    		
    		List<InvoiceJSONModel> allIteminInvoice=invoices.stream().filter(invItem -> invItem.getInvoiceno().equalsIgnoreCase(invoiceItem.getInvoiceno())).collect(Collectors.toList());
    		int serial=1;
    		for(InvoiceJSONModel itemList : allIteminInvoice)
    		{ ItemModel item= new ItemModel();
    			item.setSerialNo(serial++ + "");   			
    			item.setPname(itemList.getItemnm()+ " " +itemList.getPname());
    			item.setHsn(itemList.getHsn());
    			item.setPack(itemList.getPksize() + " " + itemList.getUnitofmesu());
    			item.setNop(itemList.getNoofunit());
    			item.setQty(itemList.getNoofunit()*itemList.getPksize());  
    			item.setValBefTax(Double.parseDouble(new DecimalFormat("##.##").format(itemList.getNetrate()*itemList.getNoofunit())));  			
    			item.setRate(Double.valueOf(itemList.getNetrate()));
    			item.setCgst(Double.parseDouble(new DecimalFormat("##.##").format(itemList.getOutputcgst()*itemList.getNoofunit())));
    			item.setSgst(Double.parseDouble(new DecimalFormat("##.##").format(itemList.getOutputsgst()*itemList.getNoofunit())));
    			item.setTotal(Double.valueOf(itemList.getTcost()));
    			item.setTaxRt(Double.valueOf(itemList.getCgstrt()) + Double.valueOf(itemList.getSgstrt()));
    			invItems.add(item);
    			totVBP += item.getValBefTax();
    			totalqty += item.getQty();
    			totNop += item.getNop();
    			total += item.getTotal();
    			cgst += item.getCgst();
    			sgst += item.getSgst();
    		
    		}
    		invoice.setTotalNop(Double.parseDouble(new DecimalFormat("##.##").format(totNop)));
    		invoice.setTotalvbt(Double.parseDouble(new DecimalFormat("##.##").format(totVBP)));
    		invoice.setTotqty(Double.parseDouble(new DecimalFormat("##.##").format(totalqty)));
    		invoice.setItems(invItems);   
    		invoice.setTotal(Double.parseDouble(new DecimalFormat("##.##").format(total)));    	
    		invoice.setTotalCgst(Double.parseDouble(new DecimalFormat("##.##").format(cgst))); 
    		invoice.setTotalSgst(Double.parseDouble(new DecimalFormat("##.##").format(sgst))); 
    		
    		allInvoices.add(invoice) ;
    		}
    	
    	} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return allInvoices;
    	
    }
    
    
}

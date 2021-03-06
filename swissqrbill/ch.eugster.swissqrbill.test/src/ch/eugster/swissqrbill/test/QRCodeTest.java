package ch.eugster.swissqrbill.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.eugster.swissqrbill.SwissQRBillGenerator;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;

public class QRCodeTest 
{
	private ObjectMapper mapper;
	
	private String output;

	private String invoice;
	
	private String sid = "123456";
	
	private int iid = 123456;
	
	private int filesizeOnlyBill = 0;
	
	private int filesizeWithInvoice = 0;
	
	@BeforeEach
	public void beforeEach() throws URISyntaxException, IOException
	{
		URI uri = QRCodeTest.class.getResource("/invoice.pdf").toURI();
		File source = Paths.get(uri).toFile();
		FileUtils.copyFile(source, new File(System.getProperty("user.home") + File.separator + "Documents/invoice.pdf"));

		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			this.output = File.separator + "Macintosh HD" + System.getProperty("java.io.tmpdir") + File.separator + "QRBill.pdf";
			this.invoice = File.separator + "Macintosh HD" + System.getProperty("user.home") + File.separator + "Documents/invoice.pdf";
			this.filesizeOnlyBill = 36725;
			this.filesizeWithInvoice = 16640;
		}
		else if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			this.output = File.separator + System.getProperty("java.io.tmpdir") + "QRBill.pdf";
			this.invoice = File.separator + System.getProperty("user.home") + File.separator + "Documents" + File.separator + "invoice.pdf";
			this.filesizeOnlyBill = 40776;
			this.filesizeWithInvoice = 16640;
		}
		this.mapper = new ObjectMapper();

	}

	@AfterEach
	public void afterEach()
	{
		try
		{
			File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "QRBill.pdf");
			if (file.exists())
			{
				file.delete();
			}
		}
		catch (Exception e)
		{
		}
		try
		{
			File file = new File(System.getProperty("user.home") + File.separator + "Documents/invoice.pdf");
			if (file.exists())
			{
				file.delete();
			}
		}
		catch (Exception e)
		{
		}
	}
	
	@Test
	public void testWithInvoiceAsNumberAndDebtorNumber() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "123451234567");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testWithDataFromFileMaker() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		int iid = this.iid;
		this.iid = 44911;
		node.put("invoice", iid);
		this.iid = iid;
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		JsonNode errorNode = targetNode.get("errors");
		assertNotNull(errorNode);
		assertEquals(1, errorNode.size());
		Iterator<JsonNode> entries = errorNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("123456", fieldname);
				assertEquals("'iban' muss die IBAN oder QR-IBAN des Rechnungstellers enthalten.", next.get(fieldname).asText());
			}
		}
	}
	
	@Test
	public void testWithEmptyStringInvoice() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", "");
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", this.iid);
		node.put("reference", "123451234567");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testWithReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("invoice", iid);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		node.put("reference", "123451234567");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testIbanWithReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH6309000000901197203");
		node.put("amount", 199.95);
		node.put("invoice", iid);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		node.put("reference", "123451234567");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
		assertEquals(NullNode.class, targetNode.get("reference").getClass());
	}
	
	@Test
	public void testQRIbanWithoutReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("invoice", iid);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals(NullNode.class, targetNode.get("reference").getClass());
		assertEquals("ERROR", targetNode.get("result").asText());	
		ArrayNode errors = ArrayNode.class.cast(targetNode.get("errors"));
		assertEquals(1, errors.size());
		assertEquals("'reference' muss eine 27-stellige Referenznummer sein, wenn QR-IBAN verwendet wird.", errors.iterator().next().get(String.valueOf(iid)).asText());
	}
	
	@Test
	public void testWithEmptyReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("invoice", iid);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		node.put("reference", "");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		ArrayNode errors = ArrayNode.class.cast(targetNode.get("errors"));
		assertEquals(1, errors.size());
		assertEquals("'reference' muss eine 27-stellige Referenznummer sein, wenn QR-IBAN verwendet wird.", errors.iterator().next().get(String.valueOf(iid)).asText());
	}
	
	@Test
	public void testWithoutExistingInvoiceToAppendTo() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", sid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testMissingCreditor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("invoice", sid);
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", "1234567");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		JsonNode errorNode = targetNode.get("errors");
		assertNotNull(errorNode);
		assertEquals(4, errorNode.size());
		Iterator<JsonNode> entries = errorNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals(sid, fieldname);
				if ("'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Buchstaben).".equals(next.get(fieldname).asText()))
				{
					assertTrue(true);
				}
				else if ("'creditor.country' muss den zweistelligen Landcode gem??ss ISO 3166 des Rechnungstellers enthalten.".equals(next.get(fieldname).asText()))
				{	
					assertTrue(true);
				}
				else
				{
					assertTrue(false);
				}
			}
		}
	}
	
	@Test
	public void testMissingDebtor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("invoice", sid);
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement f??r 2020");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testWithoutInvoice() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "123451234567");
		node.put("iban", "CH4431999123000889012");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		JsonNode errorNode = targetNode.get("errors");
		assertNotNull(errorNode);
		assertEquals(1, errorNode.size());
		Iterator<JsonNode> entries = errorNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals("Rechnungsnummer", fieldname);
				assertEquals("'invoice' Eine Rechnungsnummer muss zwingend vorhanden sein.", next.get(fieldname).asText());
			}
		}
	}

	@Test
	public void testWithoutIban() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", sid);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		JsonNode errorNode = targetNode.get("errors");
		assertNotNull(errorNode);
		assertEquals(1, errorNode.size());
		Iterator<JsonNode> entries = errorNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals(sid, fieldname);
				assertEquals("'iban' muss die IBAN oder QR-IBAN des Rechnungstellers enthalten.", next.get(fieldname).asText());
			}
		}
	}

	@Test
	public void testDocumentToAppendToDoesNotExist() throws JsonMappingException, JsonProcessingException
	{
		String nonExistentPath = (System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".pdf").replace('\\', '/');
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", nonExistentPath);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("ERROR", targetNode.get("result").asText());
		JsonNode errorNode = targetNode.get("errors");
		assertNotNull(errorNode);
		assertEquals(1, errorNode.size());
		Iterator<JsonNode> entries = errorNode.elements();
		while (entries.hasNext())
		{
			ObjectNode next = ObjectNode.class.cast(entries.next());
			Iterator<String> fields = next.fieldNames();
			while (fields.hasNext())
			{
				String fieldname = fields.next();
				assertEquals(sid, fieldname);
				assertEquals("Die Quelldatei existiert nicht. Sie muss f??r die Verarbeitung vorhanden sein.", next.get(fieldname).asText());
			}
		}
	}
	
	@Test
	public void testWithBillAsPng() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
		assertTrue(ObjectNode.class.isInstance(targetNode.get("file")));
		ObjectNode targetFileNode = ObjectNode.class.cast(targetNode.get("file"));
		assertEquals(3, targetFileNode.size());
		assertNotNull(targetFileNode.get("qrbill").asText());
		assertEquals(this.filesizeWithInvoice, targetFileNode.get("size").asInt());
		assertEquals("QRBill_" + String.valueOf(this.iid) + "." + GraphicsFormat.PNG.name().toLowerCase(), targetFileNode.get("name").asText());
	}
	
	@Test
	public void testWithBillAsPngAlone() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "123451234567");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
		assertTrue(ObjectNode.class.isInstance(targetNode.get("file")));
		ObjectNode targetFileNode = ObjectNode.class.cast(targetNode.get("file"));
		assertEquals(3, targetFileNode.size());
		assertNotNull(targetFileNode.get("qrbill").asText());
		assertEquals("QRBill_" + String.valueOf(this.iid) + "." + GraphicsFormat.PNG.name().toLowerCase(), targetFileNode.get("name").asText());
	}
	
	@Test
	public void testWithoutAmount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testWith0Amount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 0);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testQRBillOnly() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
	
	@Test
	public void testQRBillWithFileMakerPath() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testQRBillWithFileMakerPathAndInvoice() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testQRBillWithFilePath() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testQRBillWithMissingCurrency() throws JsonMappingException, JsonProcessingException
	{
		this.output = (System.getProperty("java.io.tmpdir") + UUID.randomUUID() + ".png");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
//		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement f??r 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());
		assertNull(targetNode.get("errors"));
	}
	
	@Test
	public void testQRBillAida() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH8030000001900073628");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", iid);
		node.put("reference", "3139471430009017");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Aida ??? Die Schule f??r fremdsprachige Frauen");
		creditor.put("address", "Merkurstrasse 2");
		creditor.put("city", "9000 St. Gallen");
		creditor.put("country", "CH");
		node.put("message", "");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Song??l Abay");
		debtor.put("address", "Rabenstrasse 6");
		debtor.put("city", "9008  St. Gallen");
		debtor.put("country", "CH");
		String result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode targetNode = this.mapper.readTree(result);
		assertEquals("OK", targetNode.get("result").asText());	
	}
}

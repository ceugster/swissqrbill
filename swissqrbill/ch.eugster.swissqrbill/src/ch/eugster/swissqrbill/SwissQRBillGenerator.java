package ch.eugster.swissqrbill;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codecrete.qrbill.canvas.PDFCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.ValidationResult;

public class SwissQRBillGenerator 
{
	private ObjectMapper mapper = new ObjectMapper();

	private ObjectNode targetNode = this.mapper.createObjectNode();
	
	private ArrayNode errorNode = null;
	
	public String generate(String json) 
	{
		JsonNode sourceNode = this.checkParameter(json);
		if (sourceNode != null)
		{
			String id = null;
			try
			{
				JsonNode invoiceNode = sourceNode.get("invoice");
				if (Objects.isNull(invoiceNode) || invoiceNode.asText().isEmpty())
				{
					throw new NullPointerException("'invoice' Eine Rechnungsnummer muss zwingend vorhanden sein.");
				}
				id = invoiceNode.asText();
				this.targetNode.put("invoice", id);
			}
			catch (NullPointerException e)
			{
				this.addErrorNode("Rechnungsnummer", e.getLocalizedMessage());
			}

			JsonNode sourcePathNode = sourceNode.get("path");
			ObjectNode targetPathNode = this.targetNode.putObject("path");
			String output = sourcePathNode.get("output").asText();
			Path path = null;
			try
			{
				path = adaptFilePathname(output, mapper);
				
				targetPathNode.put("output", path.toString());
			}
			catch (Exception e)
			{
				this.addErrorNode(id, "Der Pfad für die generierte Daten muss gültig sein (Systempfad oder URI).");
			}
			Path invoice = null;
			if (!Objects.isNull(sourcePathNode) && !Objects.isNull(sourcePathNode.get("invoice")) && !sourcePathNode.get("invoice").asText().trim().isEmpty())
			{
				String invoicePathname = sourcePathNode.get("invoice").asText();
				try
				{
					invoice = adaptFilePathname(invoicePathname, this.mapper);
					targetPathNode.put("invoice", invoice.toString());
				}
				catch (Exception e)
				{
					this.addErrorNode(id, "Falls die QRBill an ein bestehendes Dokument angefügt werden soll, so muss dieses Dokument bereits bestehen und einen gültigen Namen haben.");
				}
			}

			// Setup bill
			Bill bill = new Bill();
			
			/*
			 **************************************************
			 * 
			 * Bill Format setzen
			 * 
			 **************************************************
			 */
			BillFormat format = new BillFormat();
			ObjectNode targetFormNode = this.targetNode.putObject("form");
			format.setLanguage(guessLanguage(sourceNode.get("form"), targetFormNode));
			GraphicsFormat graphicsFormat = selectGraphicsFormat(sourceNode.get("form"), targetFormNode);
			try
			{
				format.setFontFamily("Arial");
				format.setGraphicsFormat(graphicsFormat);
			}
			catch (IllegalArgumentException e)
			{
				this.addErrorNode("form.graphics_format", buildGraphicsFormatErrorMessage());
			}
			try
			{
				format.setOutputSize(selectOutputSize(invoice, sourceNode.get("form"), targetFormNode));
			}
			catch (IllegalArgumentException e)
			{
				this.addErrorNode("form.output_size", buildOutputSizeErrorMessage());
			}
			bill.setFormat(format);
			
			/*
			 **************************************************
			 * 
			 * Bill Betrag setzen, falls nicht 0
			 * 
			 **************************************************
			 */
			if (sourceNode.get("amount") != null && sourceNode.get("amount").asDouble() > 0D)
			{
				bill.setAmountFromDouble(Double.valueOf(sourceNode.get("amount").asDouble()));
				this.targetNode.put("amount", bill.getAmountAsDouble());
			}
			
			/*
			 **************************************************
			 * 
			 * Bill Währung setzen
			 * 
			 **************************************************
			 */
			bill.setCurrency(this.checkCurrency(sourceNode.get("currency")));
			this.targetNode.put("currency", bill.getCurrency());
			
			/*
			 **************************************************
			 * 
			 * Bill IBAN setzen
			 * 
			 **************************************************
			 */
			String iban = null;
			try
			{
				JsonNode ibanNode = sourceNode.get("iban");
				if (Objects.isNull(ibanNode) || ibanNode.asText().trim().isEmpty())
				{
					throw new NullPointerException("'iban' muss die IBAN oder QR-IBAN des Rechnungstellers enthalten.");
				}
				iban = ibanNode.asText();
				bill.setAccount(iban);
				this.targetNode.put("iban", iban);
			}
			catch (NullPointerException e)
			{
				this.addErrorNode(id, e.getLocalizedMessage());
			}
			
			/*
			 **************************************************
			 * 
			 * Bill Referenz setzen
			 * 
			 **************************************************
			 */
			if (!Objects.isNull(iban) && iban.length() > 6)
			{
				String reference = null;
				JsonNode referenceNode = sourceNode.get("reference");
				if (iban.charAt(4) == '3' && (iban.charAt(5) == '0' || iban.charAt(5) == '1'))
				{
					try
					{
						if (Objects.isNull(referenceNode) || referenceNode.asText().trim().isEmpty())
						{
							throw new NullPointerException("'reference' muss eine 27-stellige Referenznummer sein, wenn QR-IBAN verwendet wird.");
						}
						reference = referenceNode.asText();
						bill.createAndSetQRReference(reference);
					}
					catch (NullPointerException e)
					{
						this.addErrorNode(id, e.getLocalizedMessage());
					}
				}
				else
				{
					if (!Objects.isNull(referenceNode) && referenceNode.asText().startsWith("RF"))
					{
						reference = referenceNode.asText();
						bill.createAndSetCreditorReference(reference);
					}
					else
					{
						bill.setReferenceType(Bill.REFERENCE_TYPE_NO_REF);
					}
				}
				this.targetNode.put("reference", bill.getReference());
			}
	
			// Set creditor
			JsonNode sourceCreditorNode = sourceNode.get("creditor");
			ObjectNode targetCreditorNode = this.targetNode.putObject("creditor");
			Address creditor = new Address();
			try
			{
				creditor.setName(sourceCreditorNode.get("name").asText());
				targetCreditorNode.put("name", creditor.getName());
			}
			catch (NullPointerException e)
			{
				this.addErrorNode(id, "'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Buchstaben).");
			}
			try
			{
				creditor.setAddressLine1(sourceCreditorNode.get("address").asText());
				targetCreditorNode.put("address", creditor.getAddressLine1());
			}
			catch (NullPointerException e)
			{
				this.addErrorNode(id, "'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Buchstaben).");
			}
			try
			{
				creditor.setAddressLine2(sourceCreditorNode.get("city").asText());
				targetCreditorNode.put("city", creditor.getAddressLine2());
			}
			catch (NullPointerException e)
			{
				this.addErrorNode(id, "'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Buchstaben).");
			}
			try
			{
				creditor.setCountryCode(sourceCreditorNode.get("country").asText());
				targetCreditorNode.put("country", creditor.getCountryCode());
			}
			catch (NullPointerException e)
			{
				this.addErrorNode(id, "'creditor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungstellers enthalten.");
			}
			bill.setCreditor(creditor);

			bill.setUnstructuredMessage(sourceNode.get("message").asText());
			
			// Set debtor
			if (sourceNode.get("debtor") != null)
			{
				JsonNode sourceDebtorNode = sourceNode.get("debtor");
				ObjectNode targetDebtorNode = this.targetNode.putObject("debtor");
				Address debtor = new Address();
				try
				{
					debtor.setName(sourceDebtorNode.get("name").asText());
					targetDebtorNode.put("name", debtor.getName());
				}
				catch (NullPointerException e)
				{
					this.addErrorNode(id, "'debtor.name' muss den Namen des Rechnungempfängers enthalten (maximal 70 Buchstaben).");
				}
				try
				{
					debtor.setAddressLine1(sourceDebtorNode.get("address").asText());
					targetDebtorNode.put("address", debtor.getAddressLine1());
				}
				catch (NullPointerException e)
				{
					this.addErrorNode(id, "'debtor.address' muss die Adresse des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
				}
				try
				{
					debtor.setAddressLine2(sourceDebtorNode.get("city").asText());
					targetDebtorNode.put("city", debtor.getAddressLine2());
				}
				catch (NullPointerException e)
				{
					this.addErrorNode(id, "'debtor.city' muss Postleitzahl und Ort des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
				}
				try
				{
					debtor.setCountryCode(sourceDebtorNode.get("country").asText());
					targetDebtorNode.put("country", debtor.getCountryCode());
				}
				catch (NullPointerException e)
				{
					this.addErrorNode(id, "'debtor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungsempfängers enthalten.");
				}
				bill.setDebtor(debtor);
			}
	
			// Validate QR bill
			ValidationResult validation = QRBill.validate(bill);
			if (validation.isValid() && Objects.isNull(this.targetNode.get("errors")))
			{
				if (!Objects.isNull(invoice))
				{
					if (invoice.toFile().exists())
					{
						PDFCanvas canvas = null;
						try
						{
							byte[] targetArray = null;
							InputStream is = null;
							try
							{
								is = new FileInputStream(invoice.toFile());
								targetArray = new byte[is.available()];
								is.read(targetArray);
							}
							finally
							{
								if (is != null)
								{
									is.close();
								}
							}

							canvas = new PDFCanvas(targetArray, PDFCanvas.LAST_PAGE);
							QRBill.draw(bill, canvas);
							this.targetNode.put("result", "OK");
							ObjectNode targetFileNode = this.targetNode.putObject("file");
							targetFileNode.put("qrbill", targetArray);
							targetFileNode.put("name", "QRBill_" + this.targetNode.get("invoice").asText() + "." + graphicsFormat.name().toLowerCase());
							targetFileNode.put("size", targetArray.length);
							return this.targetNode.toString();
						}
						catch (IOException e)
						{
							this.addErrorNode(id, "Das Dokument, an das die QRBill angehängt werden soll, konnte nicht geöffnet werden.");
						}
						finally
						{
							if (canvas != null)
							{
								try
								{
									canvas.saveAs(path);
									canvas.close();
								}
								catch (IOException e)
								{
									this.addErrorNode(id, "Die Zieldatei '" + invoice.toString() + "' kann nicht gespeichert werden.");
								}
							}
						}
					}
					else
					{
						this.addErrorNode(id, "Die Quelldatei existiert nicht. Sie muss für die Verarbeitung vorhanden sein.");
					}
				}
				else
				{
					// Generate QR bill
					byte[] bytes = QRBill.generate(bill);
					try 
					{
						if (path.toFile().exists())
						{
							path.toFile().delete();
						}
						OutputStream os = null;
						try
						{
							os = new FileOutputStream(path.toFile());
							os.write(bytes);
						}
						catch ( Exception e)
						{
							e.printStackTrace();
						}
						finally
						{
							if (os != null)
							{
								os.flush();
								os.close();
							}
						}
						this.targetNode.put("result", "OK");
						ObjectNode targetFileNode = this.targetNode.putObject("file");
						targetFileNode.put("qrbill", bytes);
						targetFileNode.put("name", "QRBill_" + this.targetNode.get("invoice").asText() + "." + graphicsFormat.name().toLowerCase());
						targetFileNode.put("size", bytes.length);
						return this.targetNode.toString();
					} 
					catch (FileNotFoundException e) 
					{
						this.addErrorNode(id, "Die Zieldatei kann nicht gefunden werden.");
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
						this.addErrorNode(id, "Beim Zugriff auf die Zieldatei '" + output.toString() + "' ist ein Fehler aufgetreten.");
					} 
				}
			}
		}
		this.targetNode.put("result", "ERROR");
		return this.targetNode.toString();
	}
	
	private Language guessLanguage(JsonNode node, ObjectNode targetFormNode)
	{
		Language language = null;
		JsonNode languageNode = node.get("language");
		if (languageNode != null && languageNode.asText() != null && !languageNode.asText().trim().isEmpty())
		{
			String[] availableLanguages = new String[] { "EN", "DE", "FR", "IT" };
			for (String availableLanguage : availableLanguages)
			{
				if (availableLanguage.equals(languageNode.asText()))
				{
					language = Language.valueOf(availableLanguage);
				}
			}
		}
		if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()))
		{
			language = Language.DE;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage()))
		{
			language = Language.FR;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.ITALIAN.getLanguage()))
		{
			language = Language.IT;
		}
		else
		{
			language = Language.EN;
		}
		targetFormNode.put("language", language.name());
		return language;
	}
	
	private String checkCurrency(JsonNode currencyNode)
	{
		if (Objects.isNull(currencyNode) || (!currencyNode.asText().equals("CHF") && !currencyNode.asText().equals("EUR")))
		{
			return "CHF";
		}
		else
		{
			return currencyNode.asText();
		}
	}
	private OutputSize selectOutputSize(Path invoice, JsonNode sourceFormNode, ObjectNode targetFormNode) throws IllegalArgumentException
	{
		OutputSize outputSize = null;
		JsonNode size = sourceFormNode.get("output_size");
		if (size == null || size.asText() == null || size.asText().trim().isEmpty())
		{
			if (Objects.isNull(invoice))
			{
				outputSize = OutputSize.A4_PORTRAIT_SHEET;
			}
			else
			{
				outputSize = OutputSize.QR_BILL_EXTRA_SPACE;
			}
		}
		try
		{
			outputSize = OutputSize.valueOf(size.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildOutputSizeErrorMessage());
		}
		targetFormNode.put("output_size", outputSize.name());
		return outputSize;
	}

	private GraphicsFormat selectGraphicsFormat(JsonNode sourceFormNode, ObjectNode targetFormNode) throws IllegalArgumentException
	{
		JsonNode graphicsFormat = sourceFormNode.get("graphics_format");
		if (graphicsFormat == null || graphicsFormat.asText() == null || graphicsFormat.asText().trim().isEmpty())
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		GraphicsFormat format = null;
		try
		{
			format = GraphicsFormat.valueOf(graphicsFormat.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		if (format == null || format.getClass().isAnnotationPresent(Deprecated.class))
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		targetFormNode.put("graphics_format", format.name());
		return format;
	}
	
	private String buildOutputSizeErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (OutputSize size : OutputSize.values())
		{
			builder = builder.append(size.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'output_size' muss eines der folgenden Werte sein: " + values;
	}
	
	private String buildGraphicsFormatErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (GraphicsFormat format : GraphicsFormat.values())
		{
			builder = builder.append(format.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'graphics_format' muss eines der folgenden Werte sein: " + values;
	}
	
	private Path adaptFilePathname(String path, ObjectMapper mapper) throws Exception
	{
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			if (path.startsWith(File.separator) || path.startsWith("/"))
			{
				path = path.substring(1);
			}
		}
		Path correctedPath = Objects.isNull(path) ? null : (path.trim().isEmpty() ? null : Paths.get(path));
		if (!Objects.isNull(correctedPath))
		{
			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
			{
				if (new File(path).isAbsolute() && path.startsWith("/"))
				{
					correctedPath = Paths.get(path.substring(1));
				}
			}
			else if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
			{
				correctedPath = Paths.get(path);
				if (correctedPath.isAbsolute() && !correctedPath.toFile().getAbsolutePath().startsWith("/Users") && !correctedPath.toFile().getAbsolutePath().startsWith("/Library") && !correctedPath.toFile().getAbsolutePath().startsWith("/var"))
				{
					correctedPath = Paths.get("/", correctedPath.subpath(1, correctedPath.getNameCount()).toString());
				}
			}
			correctedPath.toFile().getParentFile().mkdirs();
		}
		return correctedPath;
	}

	private void addErrorNode(String key, String value)
	{
		if (Objects.isNull(this.errorNode))
		{
			this.errorNode = this.targetNode.putArray("errors");
		}
		this.errorNode.add(mapper.createObjectNode().put(key, value));
	}
	
	private JsonNode checkParameter(String json)
	{
		// convert JSON string to Map
		JsonNode sourceNode = null;
		try 
		{
			sourceNode = mapper.readTree(json);
			if (sourceNode == null)
			{
				this.addErrorNode("Parameter", "Der übergebene Parameter konnte nicht gelesen werden. Handelt es sich um ein Json Objekt?");
			}
		} 
		catch (IllegalArgumentException e) 
		{
			this.addErrorNode("Parameter", "Der übergebene Parameter enthält ein ungültiges Element (" + e.getLocalizedMessage() + ").");
		} 
		catch (JsonMappingException e) 
		{
			this.addErrorNode("Parameter", "Der übergebene Parameter konnte nicht als JSON Object aufgebaut werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein gültiges Json Objekt?");
		} 
		catch (JsonProcessingException e) 
		{
			this.addErrorNode("Parameter", "Der übergebene Parameter konnte nicht verarbeitet werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein Json Objekt?");
		}
		return sourceNode;
	}
	
//	private enum Parameter
//	{
//		AMOUNT("amount", false),
//		CURRENCY("currency", true),
//		IBAN("iban", true),
//		REFERENCE("reference", true),
//		PATH("path", true),
//		FORM("form", false),
//		CREDITOR("creditor", true),
//		DEBTOR("debtor", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Parameter(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//	
//	private enum Creditor
//	{
//		NAME("name", true), 
//		ADDRESS("address", true), 
//		CITY("city", true), 
//		COUNTRY("country", true);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Creditor(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//
//	private enum Debtor
//	{
//		NAME("name", true), 
//		ADDRESS("address", true), 
//		CITY("city", true), 
//		COUNTRY("country", true);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Debtor(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//	
//	private enum Form
//	{
//		GRAPHICS_FORMAT("form.graphics_format", false),
//		OUTPUT_SIZE("form.output_size", false),
//		LANGUAGE("form.language", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Form(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
//
//	private enum Path
//	{
//		OUTPUT("path.output", true), 
//		INVOICE("path.invoice", false);
//		
//		private String key;
//		
//		private boolean mandatory;
//		
//		private Path(String key, boolean mandatory)
//		{
//			this.key = key;
//			this.mandatory = mandatory;
//		}
//	}
}
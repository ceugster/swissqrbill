# swissqrbill

## What is swissqrbill

swissqrbill is a java library used for creation of swiss qrbills. It is built on the following libraries:

* [commons-logging-1.2.jar](http://commons.apache.org/proper/commons-logging/download_logging.cgi)
* [fontbox-2.0.24.jar](https://www.apache.org/dyn/closer.lua/pdfbox/2.0.24/fontbox-2.0.24.jar)
* [pdfbox-2.0.24.jar](https://www.apache.org/dyn/closer.lua/pdfbox/2.0.24/pdfbox-2.0.24.jar)
* [jackson-annotations-2.12.4.jar](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations/2.12.4)
* [jackson-core-2.12.4.jar](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core/2.12.4)
* [jackson-databind-2.12.4.jar](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.12.4)
* [qrbill-generator-2.5.3.jar](https://mvnrepository.com/artifact/net.codecrete.qrbill/qrbill-generator/2.5.3)
* [qrcodegen-1.7.0.jar](https://mvnrepository.com/artifact/io.nayuki/qrcodegen/1.7.0)

## How to use it

I built this library as a base to build a filemaker plugin, that facilitates the generation of qrbills. Copy the plugin (it will come out soon) into the extension folder of filemaker to make it available within filemaker as a script step named 'GenerateSwissQRBill. It takes as parameter a JSON object, that must have a given structure (the same applies to the java library):

path.output           (mandatory: string) the full output file path as system path or as URI
path.invoice          (optional: string) an existing invoice pdf file. The qrbill will be appended to this file, if given
form.graphics_format  (mandatory: string) one of PDF, SVG, or PNG
form.output_size      (default: string) if path.invoice is given, then QR_BILL_EXTRA_SPACE is used (appends to the invoice) else A4_PORTRAIT_SHEET. Available output sizes are: QR_BILL_ONLY, A4_PORTRAIT_SHEET, QR_CODE_ONLY, and QR_BILL_EXTRA_SPACE
iban                  (mandatory: string) the iban used
creditor              (mandatory) parent JSON node for creditor's items
creditor.name         (mandatory: string) the creditor's name (must not be empty and not longer than 70 letters)
creditor.address      (mandatory: string) the creditor's address (must not be empty and not longer than 70 letters)
creditor.city         (mandatory: string) the creditor's postal code and town (must not be empty and not longer than 70 letters)
creditor.contry       (mandatory: string) the creditor's country as DIN 3166 three letter code
reference             (mandatory: string if iban is qriban, else optional) a 27 letter numeric string
amount                (optional) as number
message               (optional: string)
debtor                (optional) parent JSON node for debtor's items (see below)
debtor.name         (mandatory if debtor is present: string) the debtor's name (must not be empty and not longer than 70 letters)
debtor.address      (mandatory if debtor is present: string) the debtor's address (must not be empty and not longer than 70 letters)
debtor.city         (mandatory if debtor is present: string) the debtor's postal code and town (must not be empty and not longer than 70 letters)
debtor.contry         (mandatory if debtor is present: string) the debtor's country as DIN 3166 three letter code

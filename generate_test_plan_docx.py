import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn
import os

def create_document():
    doc = Document()
    
    sections = doc.sections
    for section in sections:
        section.top_margin = Inches(0.95)
        section.bottom_margin = Inches(0.95)
        section.left_margin = Inches(0.95)
        section.right_margin = Inches(0.95)
        section.page_width = Inches(8.5)
        section.page_height = Inches(11.0)
        
    HEX_PRIMARY = "0A2540"
    HEX_SECONDARY = "1E3A8A"
    HEX_ACCENT_RED = "DC2626"
    HEX_ACCENT_GREEN = "16A34A"
    HEX_DARK_TEXT = "1E293B"
    HEX_LIGHT_BG = "F8FAFC"
    HEX_BORDER = "CBD5E1"
    HEX_WARNING_BG = "FEF2F2"
    HEX_INFO_BG = "EFF6FF"

    COLOR_PRIMARY = RGBColor(10, 37, 64)
    COLOR_SECONDARY = RGBColor(30, 58, 138)
    COLOR_DARK = RGBColor(30, 41, 59)
    COLOR_MUTED = RGBColor(100, 116, 139)
    COLOR_RED = RGBColor(220, 38, 38)
    COLOR_GREEN = RGBColor(22, 163, 74)

    def set_cell_background(cell, fill_hex):
        tcPr = cell._tc.get_or_add_tcPr()
        shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
        tcPr.append(shd)

    def set_cell_margins(cell, top=120, bottom=120, left=160, right=160):
        tcPr = cell._tc.get_or_add_tcPr()
        tcMar = OxmlElement('w:tcMar')
        for m, val in [('w:top', top), ('w:bottom', bottom), ('w:left', left), ('w:right', right)]:
            node = OxmlElement(m)
            node.set(qn('w:w'), str(val))
            node.set(qn('w:type'), 'dxa')
            tcMar.append(node)
        tcPr.append(tcMar)

    def set_table_borders(table, color=HEX_BORDER, sz="4", val="single"):
        tblPr = table._tbl.tblPr
        borders = parse_xml(f'''
            <w:tblBorders {nsdecls("w")}>
                <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
                <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
                <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
                <w:insideV w:val="none"/>
                <w:left w:val="none"/>
                <w:right w:val="none"/>
            </w:tblBorders>
        ''')
        tblPr.append(borders)

    def add_callout(text_list, border_hex=HEX_PRIMARY, bg_hex=HEX_INFO_BG, title=None):
        tbl = doc.add_table(rows=1, cols=1)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        tbl.autofit = False
        tbl.columns[0].width = Inches(6.6)
        
        cell = tbl.cell(0, 0)
        set_cell_background(cell, bg_hex)
        set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
        
        tcPr = cell._tc.get_or_add_tcPr()
        borders = parse_xml(f'''
            <w:tcBorders {nsdecls("w")}>
                <w:top w:val="none"/>
                <w:left w:val="single" w:sz="24" w:space="0" w:color="{border_hex}"/>
                <w:bottom w:val="none"/>
                <w:right w:val="none"/>
            </w:tcBorders>
        ''')
        tcPr.append(borders)
        
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        
        if title:
            run_title = p.add_run(f"📌 {title}\n")
            run_title.font.name = "Calibri"
            run_title.font.size = Pt(10.5)
            run_title.font.bold = True
            run_title.font.color.rgb = COLOR_PRIMARY
            
        for i, item in enumerate(text_list):
            if i > 0 or title:
                p = cell.add_paragraph()
                p.paragraph_format.space_before = Pt(2)
                p.paragraph_format.space_after = Pt(2)
                p.paragraph_format.line_spacing = 1.15
            run = p.add_run(item[0])
            run.font.name = "Calibri"
            run.font.size = Pt(9.5)
            if len(item) > 1 and item[1]:
                run.font.bold = True
            if len(item) > 2 and item[2]:
                run.font.color.rgb = item[2]
            else:
                run.font.color.rgb = COLOR_DARK
                
        sp = doc.add_paragraph()
        sp.paragraph_format.space_before = Pt(2)
        sp.paragraph_format.space_after = Pt(4)

    def add_header(title, level=1, color=COLOR_PRIMARY):
        h = doc.add_heading(level=level)
        h.paragraph_format.keep_with_next = True
        run = h.add_run(title)
        run.font.name = "Calibri"
        run.font.bold = True
        run.font.color.rgb = color
        if level == 1:
            h.paragraph_format.space_before = Pt(16)
            h.paragraph_format.space_after = Pt(6)
            run.font.size = Pt(16)
        elif level == 2:
            h.paragraph_format.space_before = Pt(12)
            h.paragraph_format.space_after = Pt(4)
            run.font.size = Pt(13)
        elif level == 3:
            h.paragraph_format.space_before = Pt(8)
            h.paragraph_format.space_after = Pt(3)
            run.font.size = Pt(11)

    def add_p(text, bold_prefix="", space_after=4, color=COLOR_DARK):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(space_after)
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = "Calibri"
            r_pre.font.size = Pt(10)
            r_pre.font.bold = True
            r_pre.font.color.rgb = COLOR_PRIMARY
        r_text = p.add_run(text)
        r_text.font.name = "Calibri"
        r_text.font.size = Pt(10)
        r_text.font.color.rgb = color
        return p

    def add_bullet(text, bold_prefix="", level=0):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.left_indent = Inches(0.25 * (level + 1))
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = "Calibri"
            r_pre.font.size = Pt(9.5)
            r_pre.font.bold = True
            r_pre.font.color.rgb = COLOR_PRIMARY
        r_text = p.add_run(text)
        r_text.font.name = "Calibri"
        r_text.font.size = Pt(9.5)
        r_text.font.color.rgb = COLOR_DARK
        return p

    def add_code_block(code_text):
        tbl = doc.add_table(rows=1, cols=1)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        tbl.autofit = False
        tbl.columns[0].width = Inches(6.6)
        cell = tbl.cell(0, 0)
        set_cell_background(cell, "1E293B")
        set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
        
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.05
        run = p.add_run(code_text)
        run.font.name = "Consolas"
        run.font.size = Pt(8.5)
        run.font.color.rgb = RGBColor(241, 245, 249)
        
        sp = doc.add_paragraph()
        sp.paragraph_format.space_before = Pt(2)
        sp.paragraph_format.space_after = Pt(4)

    # PORTADA
    title_p = doc.add_paragraph()
    title_p.paragraph_format.space_before = Pt(36)
    title_p.paragraph_format.space_after = Pt(4)
    run_inst = title_p.add_run("SISTEMA BANCARIO DIGITAL | ASEGURAMIENTO DE CALIDAD Y RESILIENCIA\n")
    run_inst.font.name = "Calibri"
    run_inst.font.size = Pt(11)
    run_inst.font.bold = True
    run_inst.font.color.rgb = COLOR_MUTED
    
    run_main_title = title_p.add_run("PLAN MAESTRO DE PRUEBAS DE ENDPOINTS (API REST)\n")
    run_main_title.font.name = "Calibri"
    run_main_title.font.size = Pt(22)
    run_main_title.font.bold = True
    run_main_title.font.color.rgb = COLOR_PRIMARY
    
    run_sub_title = title_p.add_run("Verificación Funcional (Happy Path), Pruebas de Estrés, Concurrencia y Resiliencia Extrema (Chaos & Load Testing)")
    run_sub_title.font.name = "Calibri"
    run_sub_title.font.size = Pt(13)
    run_sub_title.font.italic = True
    run_sub_title.font.color.rgb = COLOR_SECONDARY
    
    doc.add_paragraph().paragraph_format.space_after = Pt(12)
    
    meta_table = doc.add_table(rows=6, cols=2)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_table.autofit = False
    meta_table.columns[0].width = Inches(2.2)
    meta_table.columns[1].width = Inches(4.4)
    set_table_borders(meta_table)
    
    meta_data = [
        ("Proyecto", "API Bancaria Integral (Clientes, Cuentas, Autenticación y Catálogos)"),
        ("Stack Tecnológico", "Spring Boot 3, Java 21, PostgreSQL, Redis, Flyway, PBKDF2, AES-256-GCM"),
        ("Versión del Documento", "3.0 - Edición Integral de Certificación con Blindaje de Validaciones"),
        ("Tipo de Estrategia", "Tripartita: Happy Path + Blindaje de Validaciones (Cuerpo, Rutas, Params) + Chaos & Load Testing"),
        ("Herramientas de Ejecución", "JUnit 5 / MockMvc, Postman / Newman, k6 (Load & Stress), JMeter, cURL Scripts"),
        ("Fecha y Estado", "Octubre 2026 | Estado: Aprobado para Ejecución")
    ]
    for row_idx, (label, val) in enumerate(meta_data):
        row = meta_table.rows[row_idx]
        c0, c1 = row.cells[0], row.cells[1]
        set_cell_background(c0, "F1F5F9")
        set_cell_background(c1, "FFFFFF" if row_idx % 2 == 0 else "F8FAFC")
        set_cell_margins(c0, top=70, bottom=70, left=120, right=120)
        set_cell_margins(c1, top=70, bottom=70, left=120, right=120)
        
        p0 = c0.paragraphs[0]
        p0.paragraph_format.line_spacing = 1.1
        r0 = p0.add_run(label)
        r0.font.name = "Calibri"
        r0.font.size = Pt(9.5)
        r0.font.bold = True
        r0.font.color.rgb = COLOR_PRIMARY
        
        p1 = c1.paragraphs[0]
        p1.paragraph_format.line_spacing = 1.1
        r1 = p1.add_run(val)
        r1.font.name = "Calibri"
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = COLOR_DARK

    doc.add_page_break()

    # CAPÍTULO 1
    add_header("1. Objetivos y Alcance del Plan de Pruebas", level=1)
    add_p("El propósito de este documento es definir el marco formal y exhaustivo para la ejecución de pruebas sobre la API REST bancaria. Este plan abarca dos dimensiones fundamentales y complementarias:", bold_prefix="Propósito Principal: ")
    
    add_bullet("Validación de las operaciones bancarias normales bajo parámetros legítimos, verificando que la lógica de negocio, cálculos de saldos, persistencia transaccional y respuestas HTTP cumplan con los requerimientos acordados.", bold_prefix="1. Dimensión Funcional (Happy Path): ")
    add_bullet("Sometimiento agresivo del servidor a condiciones límite, peticiones masivas, malformación de payloads, violaciones de concurrencia, fuerza bruta, secuestro de sesiones y agotamiento intencional de recursos de base de datos e infraestructura para determinar el punto de quiebre (breakpoint) de la API y asegurar su resiliencia.", bold_prefix="2. Dimensión Destructiva ('Tumbar la API'): ")
    
    add_callout([
        ("OBJETIVO DE LA ESTRATEGIA DESTRUTIVA:", True, COLOR_RED),
        ("'Tumbar la API' en este contexto de aseguramiento de calidad (QA) significa desafiar proactivamente las capas de seguridad y rendimiento antes de que un fallo ocurra en producción. Si la API falla, debe hacerlo de forma segura (Fail-Safe), arrojando códigos HTTP estándar (400, 401, 409, 422, 423, 429) y nunca exponiendo trazas internas, bloqueos permanentes de tablas ni fugas de memoria en la JVM.", False, COLOR_DARK)
    ], border_hex=HEX_ACCENT_RED, bg_hex=HEX_WARNING_BG, title="Declaración de Principios de Resiliencia")

    add_header("1.1 Alcance Modular Evaluado", level=2)
    add_p("Se auditan la totalidad de los módulos que integran el ecosistema bancario:")
    add_bullet("Control de inicio de sesión con contraseñas (PBKDF2), biometría (AES-256-GCM), refresh tokens rotativos, logout, estado del booleano del servidor y control de inactividad de 300 segundos.", bold_prefix="Módulo de Autenticación y Sesión (/v1/auth): ")
    add_bullet("Onboarding integral transaccional, consulta con filtros Microsoft REST Guidelines, búsqueda por CURP/RFC/Email/Cuenta, soporte de carga modular ?include=, PUT, PATCH y eliminación lógica.", bold_prefix="Módulo de Clientes (/v1/clientes): ")
    add_bullet("Apertura de cuentas con SecureRandom de 10 dígitos, saldo disponible, libro mayor e historial de movimientos auditables en la tabla account_balance.", bold_prefix="Módulo de Cuentas y Libro Mayor (/v1/cuentas): ")
    add_bullet("Módulos complementarios de expedientes bancarios con soporte completo para operaciones CRUD.", bold_prefix="Módulos de Domicilios, Contacto y Laboral: ")
    add_bullet("Catálogos personales y catálogos geográficos sincronizados con INEGI, apoyados en arquitectura de caché Redis con fallback automático a PostgreSQL.", bold_prefix="Módulo de Catálogos e INEGI (/v1/catalogos): ")
    add_bullet("Mass Assignment Protection (bloqueo de campos desconocidos), coerción estricta de tipos numéricos/JSON, validación de variables de ruta (@PathVariable con regex/positividad), coherencia de parámetros (@RequestParam) y bloqueo de PATCH vacíos.", bold_prefix="Capa Transversal de Blindaje y Validación: ")

    # CAPÍTULO 2
    add_header("2. Matriz Exhaustiva de Cobertura de Endpoints", level=1)
    add_p("La siguiente tabla relaciona los 32 endpoints expuestos por la API, identificando el método HTTP, la ruta oficial, el controlador responsable y los tipos de pruebas asignados.")

    endpoints_matrix = [
        ("POST", "/v1/auth/login", "AuthController", "Happy Path, Brute Force (5 fails -> 423), SQLi"),
        ("POST", "/v1/auth/login-biometrico", "AuthController", "Happy Path, Biometría corrupta, Side-channel timing"),
        ("POST", "/v1/auth/biometria", "AuthController", "Happy Path, Base64 malformado, Huella duplicada"),
        ("POST", "/v1/auth/refresh", "AuthController", "Happy Path, Token expirado, Reutilización de token"),
        ("POST", "/v1/auth/logout", "AuthController", "Happy Path, Logout doble, Revocación en BD"),
        ("GET", "/v1/auth/session-status", "AuthController", "Happy Path, Verificación tras 301s de inactividad"),
        ("POST", "/v1/clientes/onboarding", "ClientController", "Happy Path, Concurrencia masiva (mismo RFC/CURP), Menores"),
        ("GET", "/v1/clientes", "ClientController", "Happy Path (filtros), Inyección SQL en parámetros, DoS masivo"),
        ("GET", "/v1/clientes/{id}", "ClientController", "Happy Path (Expediente completo / ?include modular)"),
        ("GET", "/v1/clientes/curp/{curp}", "ClientController", "Happy Path, Formato CURP inválido (17/19 caracteres)"),
        ("GET", "/v1/clientes/rfc/{rfc}", "ClientController", "Happy Path, Formato RFC alterado, Fuzzing"),
        ("GET", "/v1/clientes/correo/{email}", "ClientController", "Happy Path, Email encoding, Header injection"),
        ("GET", "/v1/clientes/cuenta/{numeroCuenta}", "ClientController", "Happy Path, Cuenta < 10 dígitos, Alfanumérico"),
        ("GET", "/v1/clientes/activos", "ClientController", "Happy Path, Consulta masiva de registros"),
        ("GET", "/v1/clientes/rango-fechas", "ClientController", "Happy Path, Rango invertido (desde > hasta), ISO inválido"),
        ("PUT", "/v1/clientes/{id}", "ClientController", "Happy Path, Intento de mutar CURP/RFC, Campos nulos"),
        ("PATCH", "/v1/clientes/{id}", "ClientController", "Happy Path, JSON Merge Patch, Campos protegidos"),
        ("DELETE", "/v1/clientes/{id}", "ClientController", "Happy Path (baja lógica), Borrado doble, Cuentas inactivas"),
        ("POST", "/v1/cuentas", "AccountController", "Happy Path, Saldo inicial negativo, Cliente inactivo"),
        ("GET", "/v1/cuentas/{numeroCuenta}", "AccountController", "Happy Path, Cuenta inexistente (404)"),
        ("GET", "/v1/cuentas/{numeroCuenta}/saldo", "AccountController", "Happy Path, Sobrecarga de lecturas concurrentes"),
        ("GET", "/v1/cuentas/activas", "AccountController", "Happy Path, Verificación de estatus"),
        ("GET", "/v1/cuentas/cliente/{idClient}", "AccountController", "Happy Path, Cuentas múltiples"),
        ("GET", "/v1/cuentas/{num}/saldos", "AccountController", "Happy Path, Auditoría libro mayor, Peticiones en bucle"),
        ("GET/PUT/PATCH/DELETE", "/v1/domicilios/*", "HomeController", "Happy Path, Validación CP, ID cruzado"),
        ("GET/PUT/PATCH/DELETE", "/v1/contact-details/*", "ContactDetailController", "Happy Path, Teléfono duplicado, Formato móvil"),
        ("GET/PUT/PATCH/DELETE", "/v1/laboral/*", "EmploymentInformationController", "Happy Path, Salario cero/negativo"),
        ("GET", "/v1/catalogos/generos", "CatalogController", "Happy Path, Caché in-memory"),
        ("GET", "/v1/catalogos/estados", "CatalogController", "Happy Path, Desconexión de Redis (fallback a BD)"),
        ("GET", "/v1/catalogos/municipios/{cve}", "CatalogController", "Happy Path, Inyección de clave estado"),
        ("POST", "/v1/catalogos/sincronizar", "CatalogController", "Happy Path, Inundación DoS, Falla API INEGI externa")
    ]

    mat_table = doc.add_table(rows=len(endpoints_matrix) + 1, cols=4)
    mat_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    mat_table.autofit = False
    mat_table.columns[0].width = Inches(1.1)
    mat_table.columns[1].width = Inches(2.2)
    mat_table.columns[2].width = Inches(1.5)
    mat_table.columns[3].width = Inches(1.8)
    set_table_borders(mat_table)

    headers = ["Método", "Endpoint", "Controlador", "Enfoque de Prueba"]
    hdr_row = mat_table.rows[0]
    for idx, name in enumerate(headers):
        cell = hdr_row.cells[idx]
        set_cell_background(cell, HEX_PRIMARY)
        set_cell_margins(cell, top=100, bottom=100, left=100, right=100)
        p = cell.paragraphs[0]
        r = p.add_run(name)
        r.font.name = "Calibri"
        r.font.size = Pt(9)
        r.font.bold = True
        r.font.color.rgb = RGBColor(255, 255, 255)

    for row_idx, item in enumerate(endpoints_matrix):
        row = mat_table.rows[row_idx + 1]
        for c_idx in range(4):
            cell = row.cells[c_idx]
            bg = HEX_LIGHT_BG if row_idx % 2 == 1 else "FFFFFF"
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            p.paragraph_format.line_spacing = 1.05
            r = p.add_run(item[c_idx])
            r.font.name = "Consolas" if c_idx in [0, 1] else "Calibri"
            r.font.size = Pt(8.5)
            if c_idx == 0:
                r.font.bold = True
                if item[0] in ["POST", "PUT"]:
                    r.font.color.rgb = COLOR_SECONDARY
                elif item[0] == "DELETE":
                    r.font.color.rgb = COLOR_RED
                else:
                    r.font.color.rgb = COLOR_GREEN
            else:
                r.font.color.rgb = COLOR_DARK

    doc.add_page_break()

    # CAPÍTULO 3: HAPPY PATH
    add_header("3. Casos de Prueba Happy Path (Flujos Operativos Ideales)", level=1)
    add_p("Las pruebas Happy Path validan que la arquitectura responda con exactitud y tiempos óptimos (< 150 ms) bajo flujos de uso legítimos y datos estructurados según las reglas de negocio bancarias.")

    happy_path_cases = [
        {
            "id": "TC-HP-01",
            "name": "Onboarding Integral de Nuevo Cliente con Apertura de Cuenta Automática",
            "endpoint": "POST /v1/clientes/onboarding",
            "desc": "Registra simultáneamente datos personales, contacto, domicilio, laborales, credenciales seguras y crea la cuenta bancaria de 10 dígitos con saldo inicial de bienvenida de $1,000.00 MXN.",
            "pre": "Servidor activo, bases de datos PostgreSQL y Redis disponibles, catálogos precargados.",
            "payload": """{
  "name": "Alejandro",
  "secondName": "Manuel",
  "lastName": "Hernández",
  "secondLastName": "Gómez",
  "birthDate": "1994-06-15",
  "curp": "HEGA940615HDFRMN09",
  "rfc": "HEGA9406159A3",
  "idGender": 1,
  "idNationality": 1,
  "idMaritalStatus": 1,
  "email": "alejandro.hernandez@banco-demo.com",
  "mobilePhone": "5512345678",
  "alternativePhone": "5587654321",
  "street": "Avenida Insurgentes Sur",
  "exteriorNumber": "1602",
  "interiorNumber": "Piso 4",
  "neighborhood": "Crédito Constructor",
  "idMunicipality": 1,
  "postalCode": "03900",
  "country": "México",
  "occupation": "Ingeniero de Software Senior",
  "company": "Tech Solutions México",
  "monthlyIncome": 65000.00,
  "initialBalance": 1000.00,
  "password": "PasswordSegura#2026",
  "biometricType": "HUELLA",
  "biometricData": "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ=="
}""",
            "expected_code": "201 Created",
            "expected_res": "JSON con objeto 'client' (idClient generado), 'primaryAccount' (numeroCuenta de 10 dígitos, status 'ACTIVA', balance 1000.00), 'contactDetail', 'home', 'employmentInformation' y 'auth'. Header 'Location' apuntando a /v1/clientes/{idClient}."
        },
        {
            "id": "TC-HP-02",
            "name": "Autenticación Tradicional por Contraseña (PBKDF2) y Activación de Sesión",
            "endpoint": "POST /v1/auth/login",
            "desc": "El cliente introduce su correo electrónico y contraseña en texto plano; el servidor verifica el hash PBKDF2 con sal aleatoria, restablece los intentos fallidos y activa el booleano en servidor (login = true).",
            "pre": "Cliente previamente registrado en el sistema con credenciales activas.",
            "payload": """{
  "email": "alejandro.hernandez@banco-demo.com",
  "password": "PasswordSegura#2026"
}""",
            "expected_code": "200 OK",
            "expected_res": "Token JWT en 'accessToken', token de rotación en 'refreshToken', 'tokenType': 'Bearer', 'expiresIn': 3600, datos básicos del cliente y 'serverLoginStatus': true."
        },
        {
            "id": "TC-HP-03",
            "name": "Auditoría de Sesión en el Servidor y Temporizador de Inactividad",
            "endpoint": "GET /v1/auth/session-status (o /v1/auth/estado-servidor)",
            "desc": "Monitoreo del estado global de la sesión en el servidor y consulta de los segundos restantes antes de que ocurra el timeout por inactividad de 5 minutos (300 segundos).",
            "pre": "Sesión iniciada recientemente en el servidor.",
            "payload": "N/A (Petición GET sin cuerpo)",
            "expected_code": "200 OK",
            "expected_res": "{ 'loggedIn': true, 'inactivitySeconds': 12, 'remainingSeconds': 288, 'inactivityLimitSeconds': 300, 'status': 'SESION_ACTIVA' }"
        },
        {
            "id": "TC-HP-04",
            "name": "Enrolamiento de Biometría y Posterior Login Biométrico (AES-256-GCM)",
            "endpoint": "POST /v1/auth/biometria  seguido de  POST /v1/auth/login-biometrico",
            "desc": "1. El cliente registra su firma biométrica (Touch ID o Face ID) codificada en Base64. 2. En peticiones posteriores, el cliente realiza login biométrico sin digitar contraseña.",
            "pre": "Cliente autenticado en el sistema. Cadena de firma biométrica válida en Base64.",
            "payload": """// 1. Registro de Biometría (POST /v1/auth/biometria):
{
  "idClient": 1,
  "biometricType": "HUELLA",
  "biometricData": "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ=="
}

// 2. Login Biométrico (POST /v1/auth/login-biometrico):
{
  "email": "alejandro.hernandez@banco-demo.com",
  "biometricType": "HUELLA",
  "biometricData": "dGhpcy1pcy1hLXZhbGlkLWJpb21ldHJpYy1zaWduYXR1cmUtZGF0YQ=="
}""",
            "expected_code": "200 OK",
            "expected_res": "Desencriptación interna y comparación mediante tiempo constante (MessageDigest.isEqual). Retorno de nuevo Access Token y Refresh Token."
        },
        {
            "id": "TC-HP-05",
            "name": "Consulta Modular de Expediente de Cliente mediante ?include=",
            "endpoint": "GET /v1/clientes/{id}?include=contact,accounts,home",
            "desc": "Recuperación selectiva de módulos para optimización del ancho de banda en clientes móviles según Microsoft REST Guidelines.",
            "pre": "Cliente existente en base de datos. Servidor con login = true.",
            "payload": "N/A (GET con Query Parameter ?include=contact,accounts,home)",
            "expected_code": "200 OK",
            "expected_res": "JSON conteniendo exclusivamente los nodos 'client', 'contactDetail', 'home' y 'accounts'. Los nodos no solicitados (ej: 'employment') deben presentarse como null o no incluidos."
        },
        {
            "id": "TC-HP-06",
            "name": "Consulta de Saldo y Verificación de Libro Mayor (account_balance)",
            "endpoint": "GET /v1/cuentas/{numeroCuenta}/saldo  y  GET /v1/cuentas/{numeroCuenta}/saldos",
            "desc": "Auditoría de los movimientos contables registrados automáticamente al crear la cuenta bancaria.",
            "pre": "Cuenta creada con saldo de apertura de $1,000.00 MXN.",
            "payload": "N/A (Peticiones GET)",
            "expected_code": "200 OK",
            "expected_res": "1. Saldo disponible: $1,000.00, estatus: 'ACTIVA'. 2. Historial de saldos: Lista de movimientos conteniendo al menos un registro con movementType: 'APERTURA', amount: 1000.00, previousBalance: 0.00, currentBalance: 1000.00."
        },
        {
            "id": "TC-HP-07",
            "name": "Actualización Parcial (PATCH) de Domicilio y Datos de Contacto",
            "endpoint": "PATCH /v1/domicilios/{id}  y  PATCH /v1/contact-details/{id}",
            "desc": "Modificación únicamente de los campos alterados (teléfono móvil o calle) sin necesidad de enviar la entidad completa.",
            "pre": "Registro existente de domicilio y contacto.",
            "payload": """// PATCH /v1/domicilios/1
{
  "street": "Avenida Insurgentes Sur Corregida",
  "exteriorNumber": "1604"
}""",
            "expected_code": "200 OK",
            "expected_res": "Entidad actualizada reflejando los nuevos valores, manteniendo intactos el código postal, estado y municipio."
        },
        {
            "id": "TC-HP-08",
            "name": "Rotación de Refresh Token y Cierre de Sesión (Logout)",
            "endpoint": "POST /v1/auth/refresh  seguido de  POST /v1/auth/logout?email={email}",
            "desc": "Renovación transparente del token JWT y posterior revocación en base de datos junto con el cambio del estado del servidor a login = false.",
            "pre": "Refresh token emitido en el login vigente.",
            "payload": """{
  "refreshToken": "d748fbb2-901c-439d-b8df-9f3b50871212"
}""",
            "expected_code": "200 OK (Refresh) y 204 No Content (Logout)",
            "expected_res": "Refresh entrega nuevo token JWT. Logout invalida el token en PostgreSQL y conmuta el booleano del servidor a login = false."
        },
        {
            "id": "TC-HP-09",
            "name": "Consulta de Catálogos con Caché en Redis y Fallback Transparente",
            "endpoint": "GET /v1/catalogos/estados  y  GET /v1/catalogos/municipios/09",
            "desc": "Consulta rápida de entidades federativas y municipios desde la memoria caché de Redis.",
            "pre": "Redis operativo y catálogos sincronizados.",
            "payload": "N/A (Peticiones GET)",
            "expected_code": "200 OK",
            "expected_res": "Lista de las 32 entidades federativas y los 16 municipios de la CDMX con latencia < 35 ms."
        }
    ]

    for tc in happy_path_cases:
        add_header(f"{tc['id']}: {tc['name']}", level=2)
        add_p(tc['desc'], bold_prefix="Descripción: ")
        add_bullet(tc['endpoint'], bold_prefix="Endpoint Evaluado: ")
        add_bullet(tc['pre'], bold_prefix="Precondiciones: ")
        add_bullet(tc['expected_code'], bold_prefix="Código HTTP Esperado: ")
        add_bullet(tc['expected_res'], bold_prefix="Respuesta y Criterio: ")
        
        if tc['payload'] != "N/A":
            add_p("Payload / Solicitud Enviada:", bold_prefix="")
            add_code_block(tc['payload'])

    doc.add_page_break()

    # CAPÍTULO 3.1: BLINDAJE DE VALIDACIONES Y PROTECCIÓN DE ENTRADAS
    add_header("3.1 Matriz de Blindaje de Validaciones de Entrada (Rutas, Parámetros y Cuerpos)", level=1, color=COLOR_SECONDARY)
    add_p("En cumplimiento estricto de las mejores prácticas de seguridad REST y mitigación de asignación masiva (Mass Assignment), todos los endpoints de la API cuentan con un blindaje integral en sus tres superficies de entrada: variables de ruta (@PathVariable), parámetros de consulta (@RequestParam) y cuerpos JSON (@RequestBody).")

    add_callout([
        ("POLÍTICAS DE VALIDACIÓN Y CONTROL DE ENTRADA:", True, COLOR_SECONDARY),
        ("1. Mass Assignment Protection: spring.jackson.deserialization.fail-on-unknown-properties=true rechaza automáticamente cualquier propiedad desconocida en el body devolviendo HTTP 400 con código INVALID_FIELD y el nombre del campo objetado.", False, COLOR_DARK),
        ("2. Coerción Tipográfica Estricta: Se prohíbe el envío de cadenas de texto donde se esperan valores numéricos (montos, IDs, saldos), respondiendo con HTTP 400 e INVALID_TYPE.", False, COLOR_DARK),
        ("3. Validación de Rutas y Filtros: Variables de ruta numéricas deben ser enteros estrictamente positivos (> 0). Identificadores como CURP, RFC, número de cuenta (10 dígitos) y clave INEGI (2 dígitos) deben cumplir sus expresiones regulares o son rechazados con HTTP 400.", False, COLOR_DARK),
        ("4. Coherencia en Rangos y PATCH: Consultas por rango de fechas deben recibir obligatoriamente ambos parámetros (desde y hasta) con coherencia cronológica. Peticiones PATCH con cuerpo vacío ({}) son rechazadas con HTTP 422.", False, COLOR_DARK),
        ("5. Inmutabilidad Cívica: Los campos CURP y RFC no pueden ser modificados bajo ninguna circunstancia a través de PUT o PATCH.", False, COLOR_DARK)
    ], border_hex=HEX_SECONDARY, bg_hex=HEX_INFO_BG, title="Principios del Blindaje de Entrada")

    validation_cases = [
        {
            "id": "TC-VAL-01",
            "name": "Rechazo Inmediato de Campos Desconocidos / Mass Assignment (INVALID_FIELD)",
            "endpoint": "POST /v1/clientes/onboarding,  PUT /v1/clientes/{id},  PATCH /v1/clientes/{id}",
            "desc": "Intento de enviar atributos inexistentes o no autorizados dentro del cuerpo JSON (ej: 'role', 'isAdmin', 'campoHacker').",
            "pre": "Servidor con fail-on-unknown-properties activo.",
            "payload": """{
  "name": "Juan",
  "lastName": "Pérez",
  "campoNoPermitido": "intento_inyeccion"
}""",
            "expected_code": "400 Bad Request",
            "expected_res": "{ 'error': { 'code': 'INVALID_FIELD', 'message': 'El campo \\'campoNoPermitido\\' no está permitido o no existe en este modelo de datos', 'target': 'campoNoPermitido' } }"
        },
        {
            "id": "TC-VAL-02",
            "name": "Desajuste de Tipo de Dato en Cuerpo de Solicitud (INVALID_TYPE)",
            "endpoint": "POST /v1/cuentas,  PUT /v1/laboral/{id},  POST /v1/clientes/onboarding",
            "desc": "Envío de cadenas de texto en campos que requieren números enteros o decimales (ej: 'monthlyIncome': '75000.00' o 'idClient': 'uno').",
            "pre": "Endpoint esperando DTO con tipos numéricos estrictos.",
            "payload": """{
  "occupation": "Tech Lead",
  "company": "Banco Tech Corp",
  "monthlyIncome": "75000.00"
}""",
            "expected_code": "400 Bad Request",
            "expected_res": "{ 'error': { 'code': 'INVALID_TYPE', 'message': 'El campo \\'monthlyIncome\\' debe ser de tipo numérico (BigDecimal)...', 'target': 'monthlyIncome' } }"
        },
        {
            "id": "TC-VAL-03",
            "name": "Validación Rigurosa de Variables de Ruta (@PathVariable)",
            "endpoint": "GET /v1/clientes/{id},  GET /v1/cuentas/{numeroCuenta},  GET /v1/catalogos/municipios/{cveEnt}",
            "desc": "Verificación de validaciones de ruta: IDs no numéricos ('abc' -> INVALID_TYPE), IDs negativos o cero ('-5' -> INVALID_PARAM), número de cuenta que no tiene 10 dígitos, o clave de entidad federativa que no tiene 2 dígitos.",
            "pre": "Controladores anotados con @Validated y restricciones @Positive y @Pattern.",
            "payload": "N/A (Peticiones con rutas /v1/clientes/no-es-numero, /v1/clientes/-5, /v1/cuentas/12345, /v1/catalogos/municipios/1234)",
            "expected_code": "400 Bad Request",
            "expected_res": "1. Para texto en ID: code 'INVALID_TYPE', target 'id'. 2. Para ID negativo: code 'BAD_REQUEST', detail 'mayor a 0'. 3. Para cuenta corta: detail 'exactamente 10 dígitos numéricos'. 4. Para clave entidad: detail 'exactamente 2 dígitos numéricos'."
        },
        {
            "id": "TC-VAL-04",
            "name": "Validación de Parámetros de Consulta (@RequestParam) y Rango de Fechas",
            "endpoint": "GET /v1/clientes?curp=...,  GET /v1/clientes?desde=...&hasta=...,  POST /v1/auth/logout?email=...",
            "desc": "Evaluación de parámetros de búsqueda: email malformado, CURP/RFC con formato inválido, envío incompleto del rango de fechas (solo 'desde') o rango invertido ('desde' posterior a 'hasta').",
            "pre": "Filtros implementados con validaciones @Pattern, @Email y control de coherencia de fechas.",
            "payload": "N/A (GET /v1/clientes?desde=2026-12-31T00:00:00Z&hasta=2026-01-01T00:00:00Z)",
            "expected_code": "400 Bad Request  /  422 Unprocessable Entity",
            "expected_res": "1. Email inválido: 400 con mensaje de formato inválido. 2. Fecha invertida: 422 con 'La fecha inicial \\'desde\\' no puede ser posterior a la fecha final \\'hasta\\''. 3. Fecha incompleta: 422 con 'debe proporcionar ambos parámetros: \\'desde\\' y \\'hasta\\' '."
        },
        {
            "id": "TC-VAL-05",
            "name": "Rechazo de Peticiones de Actualización Parcial Vacías (PATCH {})",
            "endpoint": "PATCH /v1/clientes/{id},  PATCH /v1/domicilios/{id},  PATCH /v1/contact-details/{id},  PATCH /v1/laboral/{id}",
            "desc": "Garantiza que una petición de actualización parcial no sea un payload vacío {} sin ninguna instrucción de modificación.",
            "pre": "Servicios con comprobación request.isEmpty().",
            "payload": "{}",
            "expected_code": "422 Unprocessable Entity",
            "expected_res": "{ 'error': { 'code': 'BUSINESS_VALIDATION_ERROR', 'message': 'Debe proporcionar al menos un campo válido para actualizar', 'target': 'requestBody' } }"
        },
        {
            "id": "TC-VAL-06",
            "name": "Protección de Inmutabilidad Cívica (CURP y RFC en PUT y PATCH)",
            "endpoint": "PUT /v1/clientes/{id}  y  PATCH /v1/clientes/{id}",
            "desc": "Intento malicioso o accidental de cambiar los datos de identidad cívica permanente (CURP o RFC) una vez registrado el cliente.",
            "pre": "Cliente previamente dado de alta en base de datos.",
            "payload": """{
  "curp": "HEGA940615HDFRMN99",
  "rfc": "HEGA9406159A9"
}""",
            "expected_code": "422 Unprocessable Entity",
            "expected_res": "{ 'error': { 'code': 'BUSINESS_VALIDATION_ERROR', 'message': 'No está permitido modificar la CURP del cliente' (o RFC), 'target': 'curp' } }"
        }
    ]

    for tc in validation_cases:
        add_header(f"{tc['id']}: {tc['name']}", level=2, color=COLOR_SECONDARY)
        add_p(tc['desc'], bold_prefix="Descripción: ")
        add_bullet(tc['endpoint'], bold_prefix="Endpoint Evaluado: ")
        add_bullet(tc['pre'], bold_prefix="Precondiciones: ")
        add_bullet(tc['expected_code'], bold_prefix="Código HTTP Esperado: ")
        add_bullet(tc['expected_res'], bold_prefix="Respuesta y Criterio: ")
        
        if tc['payload'] != "N/A":
            add_p("Payload / Solicitud Enviada:", bold_prefix="")
            add_code_block(tc['payload'])

    doc.add_page_break()

    # CAPÍTULO 4: PRUEBAS DE ESTRÉS
    add_header("4. Plan de Pruebas Destructivas, Estrés y Resiliencia ('Tumbar la API')", level=1, color=COLOR_RED)
    
    add_callout([
        ("OBJETIVO DEL CAPÍTULO DE CAOS Y ESTRÉS:", True, COLOR_RED),
        ("En esta sección se detallan 10 escenarios diseñados deliberadamente para llevar la API al límite operativo. Se evalúan condiciones de carrera (Race Conditions), saturación de hilos en HikariCP, ataques de fuerza bruta, bombas de JSON, agotamiento de sockets HTTP y fallas de componentes de infraestructura (Redis).", False, COLOR_DARK),
        ("Criterio de Resiliencia: La API jamás debe responder con errores no controlados (500 Internal Server Error sin estructura), no debe permitir la corrupción del libro mayor y debe recuperarse inmediatamente tras el cese de la carga extrema.", False, COLOR_DARK)
    ], border_hex=HEX_ACCENT_RED, bg_hex=HEX_WARNING_BG, title="Protocolo de Pruebas de Esfuerzo Crítico")

    stress_cases = [
        {
            "id": "TC-STR-01",
            "name": "Ataque de Fuerza Bruta y Bloqueo de Cuenta (Brute-Force & Lockout)",
            "endpoint": "POST /v1/auth/login",
            "vector": "Envío de 5 peticiones consecutivas con contraseña errónea para un usuario válido, seguido de un 6to intento.",
            "impact": "Saturación del algoritmo hash criptográfico PBKDF2 (computacionalmente costoso en CPU) y bloqueo preventivo del cliente.",
            "test_steps": """1. Ejecutar en bucle 5 peticiones HTTP POST con contraseña incorrecta 'FakePass#123'.
2. Verificar que cada petición falle con código HTTP 401 Unauthorized y decremente los intentos permitidos.
3. Al 5to intento fallido, la cuenta debe ser bloqueada automáticamente en la base de datos (failed_attempts = 5, is_locked = true).
4. Ejecutar el 6to intento (incluso usando la contraseña CORRECTA).""",
            "expected_behavior": "El 6to intento DEBE arrojar HTTP 423 Locked con mensaje 'Cuenta bloqueada por exceso de intentos fallidos. Contacte a soporte bancario'. El uso de CPU no debe superar el 75% durante el ataque gracias a rate-limiting o backoff.",
            "script_sample": """# PowerShell Loop de Ataque
1..6 | ForEach-Object {
    Invoke-RestMethod -Uri "http://localhost:8080/v1/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body '{"email":"alejandro.hernandez@banco-demo.com","password":"ClaveErronea"}' `
        -SkipHttpErrorCheck
}"""
        },
        {
            "id": "TC-STR-02",
            "name": "Condición de Carrera (Race Condition) en Creación Concurrente con Mismo RFC/CURP",
            "endpoint": "POST /v1/clientes/onboarding",
            "vector": "Disparo simultáneo de 50 hilos paralelos intentando registrar al mismo cliente con el mismo CURP y RFC en la misma fracción de milisegundo.",
            "impact": "Riesgo de saltarse las validaciones a nivel de servicio Java y generar clientes duplicados o colisiones en la tabla de cuentas.",
            "test_steps": """1. Configurar un pool de 50 Virtual Users (VU) en k6 o JMeter disparando exactamente el mismo payload de onboarding.
2. Todas las peticiones deben lanzarse al mismo tiempo sin pausa intermedia (timestamp exacto).
3. Inspeccionar el comportamiento transaccional (@Transactional) y las restricciones UNIQUE de PostgreSQL.""",
            "expected_behavior": "Exactamente 1 petición debe retornar HTTP 201 Created. Las 49 peticiones restantes DEBEN retornar HTTP 409 Conflict ('CURP o RFC ya registrado') o 400 Bad Request. Bajo ninguna circunstancia la base de datos debe contener 2 registros con el mismo RFC ni crear cuentas huérfanas.",
            "script_sample": """// Snippet de prueba k6 para colisión concurrente
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    race_condition: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '10s',
    },
  },
};

export default function () {
  const payload = JSON.stringify({
    name: 'Clon',
    lastName: 'Gomez',
    birthDate: '1994-06-15',
    curp: 'CLON940615HDFRMN09',
    rfc: 'CLON9406159A3',
    idGender: 1,
    idNationality: 1,
    idMaritalStatus: 1,
    email: 'clon@banco.com',
    mobilePhone: '5500000000',
    street: 'Calle 1',
    exteriorNumber: '1',
    neighborhood: 'Centro',
    idMunicipality: 1,
    postalCode: '03900',
    occupation: 'QA Tester',
    company: 'Test Corp',
    monthlyIncome: 20000.00,
    initialBalance: 1000.00,
    password: 'PasswordSegura#2026'
  });
  const res = http.post('http://localhost:8080/v1/clientes/onboarding', payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'status is 201 or 409': (r) => r.status === 201 || r.status === 409 });
}"""
        },
        {
            "id": "TC-STR-03",
            "name": "Agotamiento Intencional del Pool de Conexiones a Base de Datos (HikariCP Starvation)",
            "endpoint": "POST /v1/catalogos/sincronizar  y  GET /v1/clientes",
            "vector": "Bombardeo masivo de 300 peticiones concurrentes hacia endpoints de alta intensidad transaccional (sincronización batch y consultas full scan).",
            "impact": "Consumo de todos los sockets del pool HikariCP (por defecto 10-30 conexiones). Si la aplicación no gestiona los timeouts de conexión, las peticiones se congelarán produciendo timeouts en cascada.",
            "test_steps": """1. Disparar 300 peticiones concurrentes a la sincronización de catálogos y consultas de clientes simultáneamente.
2. Observar los logs del servidor buscando advertencias de 'HikariPool-1 - Connection is not available, request timed out after 30000ms'.
3. Evaluar si la aplicación degrada elegantemente o si el proceso Java se congela.""",
            "expected_behavior": "Las peticiones que superen la capacidad del pool deben ser encoladas ordenadamente o rechazadas con HTTP 503 Service Unavailable / HTTP 500 estructurado. Una vez finalizada la ráfaga, el pool debe liberarse al 100% en menos de 5 segundos sin requerir reinicio del contenedor o JVM.",
            "script_sample": """# Stress test de saturación con cURL concurrente en bash/PowerShell
for ($i=1; $i -le 300; $i++) {
    Start-Job -ScriptBlock {
        Invoke-WebRequest -Uri "http://localhost:8080/v1/catalogos/sincronizar" -Method Post -TimeoutSec 10
    }
}"""
        },
        {
            "id": "TC-STR-04",
            "name": "Bypass y Sabotaje del Interceptor de Sesión del Servidor (ServerSessionInterceptor)",
            "endpoint": "GET /v1/clientes/1  y  GET /v1/cuentas/1234567890/saldo",
            "vector": "1. Petición a endpoints protegidos sin hacer login previo. 2. Petición justo a los 301 segundos de haber iniciado sesión (superando los 300s de inactividad). 3. Peticiones simultáneas mientras se ejecuta un logout en paralelo.",
            "impact": "Comprobar que ninguna fuga de lógica permita consultar datos financieros o expedientes confidenciales si el booleano del servidor no es estrictamente 'login = true' y está vigente.",
            "test_steps": """1. Caso A (Sin Login): Enviar GET a /v1/clientes/1 en frío tras reiniciar el servidor.
2. Caso B (Timeout): Iniciar sesión en /v1/auth/login. Esperar 301 segundos sin enviar peticiones. Enviar GET a /v1/cuentas/1234567890/saldo.
3. Caso C (Logout Concurrente): Disparar 100 peticiones de consulta mientras otro hilo ejecuta /v1/auth/logout.""",
            "expected_behavior": "El interceptor DEBE cortar el tráfico inmediatamente arrojando HTTP 401 Unauthorized con el mensaje descriptivo: 'Operación restringida: Para realizar consultas en el servidor, debe iniciar sesión (login = true)'. Ningún dato sensible debe filtrarse.",
            "script_sample": """// Payload de respuesta esperada ante sesión inactiva:
{
  "timestamp": "2026-10-09T00:30:00Z",
  "status": 401,
  "error": "Acceso No Autorizado (Sesión Inactiva)",
  "message": "Operación restringida: Para realizar consultas en el servidor, debe iniciar sesión (login = true)...",
  "path": "/v1/clientes/1",
  "inactivityLimitSeconds": 300
}"""
        },
        {
            "id": "TC-STR-05",
            "name": "Bomba de Payloads (JSON Flooding, Caracteres Nulos y Fuzzing Extremo)",
            "endpoint": "POST /v1/clientes/onboarding  y  PUT /v1/clientes/1",
            "vector": "Envío de payloads anómalos: cuerpos de 15 Megabytes, árboles de JSON anidados con 200 niveles, cadenas de texto con caracteres nulos \\u0000, inyección de emojis y cadenas de 50,000 caracteres en los campos de texto.",
            "impact": "Desbordamiento de memoria (Java Heap OutOfMemoryError), fallo del parser Jackson ObjectMapper o corrupción de columnas VARCHAR en PostgreSQL.",
            "test_steps": """1. Generar un archivo JSON malformado con un string de 1,000,000 de caracteres en el campo 'firstName'.
2. Enviar un JSON que exceda el límite permitido de MaxPayloadSize de Spring Boot.
3. Enviar secuencias de inyección SQL (' OR '1'='1 --) y XSS (<script>alert(1)</script>) en CURP y Nombres.""",
            "expected_behavior": "Spring Boot y Jackson deben rechazar la petición con HTTP 400 Bad Request o HTTP 413 Payload Too Large. La JVM no debe sufrir picos anormales de Garbage Collection (GC) ni desbordamiento de memoria.",
            "script_sample": """# Generación de payload gigante para prueba de fuzzing
$hugeString = "A" * 500000
$body = @{
    name = $hugeString
    curp = "CURPINVALIDO123456"
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/v1/clientes/onboarding" -Method Post -Body $body -ContentType "application/json" -SkipHttpErrorCheck"""
        },
        {
            "id": "TC-STR-06",
            "name": "Violación Forzada de Restricciones Contables y Reglas de Negocio en Base de Datos",
            "endpoint": "POST /v1/cuentas  y  POST /v1/clientes/onboarding",
            "vector": "Intentar forzar estados financieros anómalos: 1. Crear cuenta con saldo inicial negativo (-$50,000.00). 2. Crear cuenta asociada a un cliente inactivo. 3. Registrar un cliente con fecha de nacimiento futura o menor de edad (2025). 4. Intentar modificar CURP y RFC mediante PUT.",
            "impact": "Comprobación de la doble barrera de seguridad: validaciones en capa de servicio Java vs constraints check de base de datos (ck_account_balance >= 0, ck_account_number ~ '^[0-9]{10}$').",
            "test_steps": """1. Petición POST /v1/cuentas con { idClient: 1, initialBalance: -500.00 }.
2. Petición PUT /v1/clientes/1 intentando enviar un CURP distinto al registrado.
3. Petición POST /v1/clientes/onboarding con dateOfBirth: '2020-01-01' (6 años de edad).""",
            "expected_behavior": "Respuesta inmediata HTTP 400 Bad Request o 422 Unprocessable Entity ('El saldo inicial no puede ser negativo' / 'El cliente debe ser mayor de 18 años' / 'CURP y RFC no pueden ser modificados'). No debe ocurrir ninguna inserción huérfana en account_balance.",
            "script_sample": """{
  "idClient": 1,
  "initialBalance": -500.00
}
// Respuesta esperada: HTTP 422 con violación de regla de negocio o HTTP 400."""
        },
        {
            "id": "TC-STR-07",
            "name": "Prueba de Caos: Desconexión Abrupta de Redis durante Consultas de Catálogos",
            "endpoint": "GET /v1/catalogos/estados  y  GET /v1/catalogos/municipios/09",
            "vector": "Detener el contenedor o servicio de Redis mientras se ejecutan 1,000 peticiones por segundo de consulta de catálogos.",
            "impact": "Verificar la arquitectura de alta disponibilidad y fallback. Si Redis cae, ¿la API se cae o sigue funcionando consultando PostgreSQL directamente?",
            "test_steps": """1. Iniciar tráfico de lectura constante contra /v1/catalogos/estados.
2. Ejecutar comando 'docker stop redis' o 'redis-cli shutdown' en caliente.
3. Monitorear los logs y el tiempo de respuesta de los siguientes 500 requests.
4. Reiniciar Redis y verificar la reanudación del almacenamiento en caché.""",
            "expected_behavior": "CERO caídas (0% Error Rate). El CatalogService debe capturar la excepción de conexión de Redis, registrar un log de advertencia y redirigir la consulta de forma transparente hacia PostgreSQL. La latencia puede subir de 15ms a 80ms pero el usuario jamás percibe un error 500.",
            "script_sample": """# Simulación de Caos en Docker
docker stop redis_banco_container
# Ejecutar verificación de catálogo:
curl -i http://localhost:8080/v1/catalogos/estados
# Reanudar contenedor:
docker start redis_banco_container"""
        },
        {
            "id": "TC-STR-08",
            "name": "Prueba de Carga Extrema y Picos de Tráfico (Spike Testing 1,200 Req/s con k6)",
            "endpoint": "GET /v1/clientes/activos  y  POST /v1/auth/login",
            "vector": "Inyección escalonada de carga: Ramp-up rápido de 0 a 1,200 usuarios concurrentes en 15 segundos, sostenido durante 2 minutos.",
            "impact": "Monitoreo del uso de memoria JVM, tiempo de respuesta en percentiles (p90, p95, p99), saturación de CPU y tasa de errores HTTP.",
            "test_steps": """1. Ejecutar script de k6 simulando tráfico masivo de usuarios consultando cuentas y logueándose.
2. Medir métricas de rendimiento del servidor mediante Spring Boot Actuator o VisualVM.""",
            "expected_behavior": "p95 < 250ms durante carga normal; p95 < 800ms durante el pico de 1,200 VUs. Tasa de error < 1%. El uso de heap memory debe estabilizarse sin arrojar OutOfMemoryError.",
            "script_sample": """// Script de carga k6 Spike Test
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '20s', target: 1200 },
    { duration: '1m', target: 1200 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const res = http.get('http://localhost:8080/v1/cuentas/activas');
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.1);
}"""
        },
        {
            "id": "TC-STR-09",
            "name": "Ataque Criptográfico de Canal Lateral y Plantilla Biométrica Corrupta",
            "endpoint": "POST /v1/auth/login-biometrico",
            "vector": "1. Envío de cadenas Base64 truncadas o con bytes corruptos. 2. Peticiones masivas intentando medir variaciones de tiempo de respuesta (Timing Attack) para adivinar bytes de la firma biométrica.",
            "impact": "Comprobar que la desencriptación AES-256-GCM y la comparación en tiempo constante (MessageDigest.isEqual) protejan al cliente y no filtren excepciones criptográficas hacia el frontend.",
            "test_steps": """1. Enviar payload biométrico con string que no sea Base64 válido (ej: '!!!###$$$***').
2. Enviar biometría con un solo byte diferente respecto a la registrada y medir la diferencia de tiempo en 1,000 peticiones.""",
            "expected_behavior": "Respuesta controlada HTTP 400 Bad Request ('Formato Base64 inválido') o HTTP 401 Unauthorized ('Firma biométrica no coincide'). La desviación estándar en el tiempo de procesamiento debe ser < 5ms evitando ataques de canal lateral.",
            "script_sample": """{
  "email": "alejandro.hernandez@banco-demo.com",
  "biometricType": "HUELLA",
  "biometricData": "%%%ESTO_NO_ES_BASE64_VALIDO%%%"
}"""
        },
        {
            "id": "TC-STR-10",
            "name": "Baja Lógica y Verificación de Cascada de Inactivación en Operaciones Posteriores",
            "endpoint": "DELETE /v1/clientes/{id}  seguido de  POST /v1/cuentas",
            "vector": "1. Ejecutar baja lógica del cliente. 2. Intentar abrir una nueva cuenta para ese cliente. 3. Intentar iniciar sesión con las credenciales del cliente desactivado.",
            "impact": "Garantizar que al desactivar un cliente, todas sus cuentas pasen a status = 'INACTIVA' y se bloquee cualquier transacción posterior sin destruir la historia contable en account_balance.",
            "test_steps": """1. DELETE /v1/clientes/1 -> 204 No Content.
2. POST /v1/cuentas para idClient: 1.
3. POST /v1/auth/login para el correo del cliente desactivado.""",
            "expected_behavior": "La creación de cuenta posterior debe ser rechazada con HTTP 400 Bad Request ('No se puede abrir una cuenta para un cliente inactivo'). El login debe arrojar HTTP 400 ('La cuenta del cliente se encuentra inactiva'). La información no debe ser borrada físicamente (Soft Delete).",
            "script_sample": """// Intento de login tras baja lógica
{
  "email": "cliente.eliminado@banco-demo.com",
  "password": "Password123#"
}
// Respuesta esperada: HTTP 400 Bad Request con mensaje de cuenta inactiva."""
        }
    ]

    for tc in stress_cases:
        add_header(f"{tc['id']}: {tc['name']}", level=2, color=COLOR_RED)
        add_bullet(tc['endpoint'], bold_prefix="Endpoint Vulnerado: ")
        add_bullet(tc['vector'], bold_prefix="Vector de Ataque / Estrés: ")
        add_bullet(tc['impact'], bold_prefix="Riesgo / Impacto Potencial: ")
        add_p(tc['test_steps'], bold_prefix="Paso a Paso de Ejecución:\n")
        add_p(tc['expected_behavior'], bold_prefix="Comportamiento Esperado (Criterio de Resiliencia):\n", color=COLOR_PRIMARY)
        add_p("Script / Herramienta de Demostración:", bold_prefix="")
        add_code_block(tc['script_sample'])

    doc.add_page_break()

    # CAPÍTULO 5
    add_header("5. Criterios de Aceptación, SLAs y Clasificación de Defectos", level=1)
    add_p("Para dar por certificada la API bancaria, los resultados de ejecución deben cumplir estrictamente con los acuerdos de nivel de servicio (SLAs) técnicos:")

    sla_data = [
        ("Operaciones de Lectura (GET simples)", "< 80 ms", "< 150 ms", "99.9%"),
        ("Consultas Modulares Complejas (?include=)", "< 120 ms", "< 250 ms", "99.5%"),
        ("Transacción de Onboarding (POST completo)", "< 200 ms", "< 450 ms", "99.0%"),
        ("Autenticación Criptográfica (PBKDF2/AES)", "< 150 ms", "< 350 ms", "99.5%"),
        ("Saturación Bajo Estrés (Spike 1,200 VU)", "< 500 ms", "< 900 ms", "99.0%"),
        ("Disponibilidad Global de la API", "N/A", "N/A", "99.95%")
    ]

    sla_table = doc.add_table(rows=len(sla_data) + 1, cols=4)
    sla_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    sla_table.autofit = False
    sla_table.columns[0].width = Inches(2.6)
    sla_table.columns[1].width = Inches(1.3)
    sla_table.columns[2].width = Inches(1.3)
    sla_table.columns[3].width = Inches(1.4)
    set_table_borders(sla_table)

    sla_headers = ["Tipo de Transacción", "Tiempo Promedio", "Percentil 95 (p95)", "Tasa de Éxito"]
    for idx, h_name in enumerate(sla_headers):
        cell = sla_table.rows[0].cells[idx]
        set_cell_background(cell, HEX_PRIMARY)
        set_cell_margins(cell, top=90, bottom=90, left=90, right=90)
        p = cell.paragraphs[0]
        r = p.add_run(h_name)
        r.font.name = "Calibri"
        r.font.size = Pt(9)
        r.font.bold = True
        r.font.color.rgb = RGBColor(255, 255, 255)

    for r_idx, row_data in enumerate(sla_data):
        row = sla_table.rows[r_idx + 1]
        for c_idx in range(4):
            cell = row.cells[c_idx]
            bg = HEX_LIGHT_BG if r_idx % 2 == 1 else "FFFFFF"
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            r = p.add_run(row_data[c_idx])
            r.font.name = "Calibri"
            r.font.size = Pt(9)
            if c_idx == 0:
                r.font.bold = True
                r.font.color.rgb = COLOR_PRIMARY
            else:
                r.font.color.rgb = COLOR_DARK

    add_header("5.1 Matriz de Severidad de Defectos", level=2)
    add_bullet("Caída de la JVM (OutOfMemoryError), inconsistencia en el libro mayor contable (saldos que no cuadran), bypass del interceptor de seguridad permitiendo ver datos sin login, o corrupción de registros de base de datos.", bold_prefix="Severidad 1 (Crítico / Blocker): ")
    add_bullet("Fallo en la creación de cuentas o clientes bajo condiciones normales, bloqueo permanente e irrecuperable de cuentas válidas, tiempos de respuesta > 2 segundos en Happy Path.", bold_prefix="Severidad 2 (Mayor / High): ")
    add_bullet("Mensaje de error confuso, código HTTP no correspondiente al estándar (ej: retornar 500 en vez de 422 o 400), fallos menores en caché Redis que no tiran la aplicación.", bold_prefix="Severidad 3 (Medio / Medium): ")
    add_bullet("Inconsistencias estéticas en la documentación de Swagger/OpenAPI o nombres de propiedades en responses no uniformes.", bold_prefix="Severidad 4 (Bajo / Low): ")

    add_header("6. Conclusión y Recomendaciones de Despliegue", level=1)
    add_p("La ejecución rigurosa de este plan dual garantiza que la API no solo funcione a la perfección en escenarios ideales de demostración, sino que sea capaz de resistir ataques coordinados, ráfagas intempestivas de tráfico y fallas de componentes de soporte.", bold_prefix="Dictamen de Aseguramiento de Calidad: ")
    add_bullet("Configurar alertas en tiempo real sobre el contador de intentos fallidos en login para mitigar ataques de fuerza bruta en fases tempranas.", bold_prefix="1. Rate Limiting en Gateway: ")
    add_bullet("Ajustar el pool HikariCP (maximum-pool-size = 30, connectionTimeout = 5000ms) según las pruebas de estrés realizadas.", bold_prefix="2. Afinación de HikariCP: ")
    add_bullet("Implementar Circuit Breakers con Resilience4j en la llamada al API de INEGI durante la sincronización batch para evitar bloqueos por latencia de servicios gubernamentales externos.", bold_prefix="3. Circuit Breaker en INEGI: ")

    output_filename = "Plan_de_Pruebas_API_Bancaria.docx"
    try:
        doc.save(output_filename)
        print(f"Documento generado exitosamente: {output_filename}")
    except PermissionError:
        output_filename = "Plan_de_Pruebas_API_Bancaria_Actualizado.docx"
        doc.save(output_filename)
        print(f"Documento guardado como: {output_filename} (el archivo principal está abierto en Microsoft Word)")

if __name__ == "__main__":
    create_document()

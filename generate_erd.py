import os
import json
import subprocess
import shutil

# Schema definition for all 12 tables of the Banco API
tables = [
    {
        "id": "state",
        "name": "state",
        "category": "catalog",
        "category_title": "CATÁLOGO",
        "desc": "Entidades federativas (INEGI)",
        "x": 60,
        "y": 80,
        "width": 310,
        "fields": [
            {"name": "id_state", "type": "SMALLINT", "pk": True, "fk": False, "uq": False},
            {"name": "cve_ent", "type": "CHAR(2)", "pk": False, "fk": False, "uq": True},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "municipality",
        "name": "municipality",
        "category": "catalog",
        "category_title": "CATÁLOGO",
        "desc": "Municipios por Estado",
        "x": 60,
        "y": 320,
        "width": 310,
        "fields": [
            {"name": "id_municipality", "type": "INTEGER", "pk": True, "fk": False, "uq": False},
            {"name": "id_state", "type": "SMALLINT", "pk": False, "fk": True, "ref": "state", "uq": False},
            {"name": "cve_mun", "type": "CHAR(3)", "pk": False, "fk": False, "uq": False},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "gender",
        "name": "gender",
        "category": "catalog",
        "category_title": "CATÁLOGO",
        "desc": "Sexo / Género (CURP H/M)",
        "x": 60,
        "y": 600,
        "width": 310,
        "fields": [
            {"name": "id_gender", "type": "SMALLINT", "pk": True, "fk": False, "uq": False},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "description", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "nationality",
        "name": "nationality",
        "category": "catalog",
        "category_title": "CATÁLOGO",
        "desc": "Nacionalidades",
        "x": 60,
        "y": 850,
        "width": 310,
        "fields": [
            {"name": "id_nationality", "type": "SMALLINT", "pk": True, "fk": False, "uq": False},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "description", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "marital_status",
        "name": "marital_status",
        "category": "catalog",
        "category_title": "CATÁLOGO",
        "desc": "Estado Civil",
        "x": 60,
        "y": 1100,
        "width": 310,
        "fields": [
            {"name": "id_marital_status", "type": "SMALLINT", "pk": True, "fk": False, "uq": False},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "description", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "home",
        "name": "home",
        "category": "details",
        "category_title": "DOMICILIO (1 : 1)",
        "desc": "Dirección fiscal del cliente",
        "x": 470,
        "y": 70,
        "width": 360,
        "fields": [
            {"name": "id_home", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "id_client", "type": "BIGINT", "pk": False, "fk": True, "ref": "client", "uq": True},
            {"name": "street", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "exterior_number", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "interior_number", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "neighborhood", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "id_municipality", "type": "INTEGER", "pk": False, "fk": True, "ref": "municipality", "uq": False},
            {"name": "postal_code", "type": "CHAR(5)", "pk": False, "fk": False, "uq": False},
            {"name": "country", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "client",
        "name": "client",
        "category": "core",
        "category_title": "ENTIDAD CENTRAL",
        "desc": "Cliente Persona Física",
        "x": 470,
        "y": 480,
        "width": 360,
        "fields": [
            {"name": "id_client", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "second_name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "last_name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "second_last_name", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "birth_date", "type": "DATE", "pk": False, "fk": False, "uq": False},
            {"name": "curp", "type": "CHAR(18)", "pk": False, "fk": False, "uq": True},
            {"name": "rfc", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "id_gender", "type": "SMALLINT", "pk": False, "fk": True, "ref": "gender", "uq": False},
            {"name": "id_nationality", "type": "SMALLINT", "pk": False, "fk": True, "ref": "nationality", "uq": False},
            {"name": "id_marital_status", "type": "SMALLINT", "pk": False, "fk": True, "ref": "marital_status", "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "deactivated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "contact_details",
        "name": "contact_details",
        "category": "details",
        "category_title": "CONTACTO (1 : 1)",
        "desc": "Teléfonos y Correo Único",
        "x": 470,
        "y": 1040,
        "width": 360,
        "fields": [
            {"name": "id_contact_detail", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "id_client", "type": "BIGINT", "pk": False, "fk": True, "ref": "client", "uq": True},
            {"name": "email", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "mobile_phone", "type": "CHAR(10)", "pk": False, "fk": False, "uq": True},
            {"name": "alternative_phone", "type": "CHAR(10)", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "employment_information",
        "name": "employment_information",
        "category": "details",
        "category_title": "LABORAL (1 : 1)",
        "desc": "Ocupación e Ingreso Mensual",
        "x": 950,
        "y": 70,
        "width": 370,
        "fields": [
            {"name": "id_employment", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "id_client", "type": "BIGINT", "pk": False, "fk": True, "ref": "client", "uq": True},
            {"name": "occupation", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "company", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "monthly_income", "type": "NUMERIC(12,2)", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "auth",
        "name": "auth",
        "category": "security",
        "category_title": "SEGURIDAD Y ACCESO",
        "desc": "Credenciales y Biometría",
        "x": 950,
        "y": 360,
        "width": 370,
        "fields": [
            {"name": "id_login", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "id_client", "type": "BIGINT", "pk": False, "fk": True, "ref": "client", "uq": False},
            {"name": "email", "type": "TEXT", "pk": False, "fk": False, "uq": True},
            {"name": "password_hash", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "biometric_type", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "biometric_template", "type": "BYTEA", "pk": False, "fk": False, "uq": False},
            {"name": "biometric_registered_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "refresh_token", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "refresh_token_expires_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "is_active", "type": "BOOLEAN", "pk": False, "fk": False, "uq": False},
            {"name": "failed_attempts", "type": "SMALLINT", "pk": False, "fk": False, "uq": False},
            {"name": "last_login_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "account",
        "name": "account",
        "category": "finance",
        "category_title": "CUENTAS BANCARIAS",
        "desc": "Cuenta de Ahorro / Cheques",
        "x": 950,
        "y": 880,
        "width": 370,
        "fields": [
            {"name": "id_account", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "account_number", "type": "CHAR(10)", "pk": False, "fk": False, "uq": True},
            {"name": "id_client", "type": "BIGINT", "pk": False, "fk": True, "ref": "client", "uq": False},
            {"name": "balance", "type": "NUMERIC(15,2)", "pk": False, "fk": False, "uq": False},
            {"name": "status", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "opened_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
            {"name": "updated_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    },
    {
        "id": "account_balance",
        "name": "account_balance",
        "category": "finance",
        "category_title": "LIBRO MAYOR",
        "desc": "Historial de saldos y transacciones",
        "x": 1420,
        "y": 880,
        "width": 380,
        "fields": [
            {"name": "id_balance", "type": "BIGINT", "pk": True, "fk": False, "uq": False},
            {"name": "id_account", "type": "BIGINT", "pk": False, "fk": True, "ref": "account", "uq": False},
            {"name": "previous_balance", "type": "NUMERIC(15,2)", "pk": False, "fk": False, "uq": False},
            {"name": "amount", "type": "NUMERIC(15,2)", "pk": False, "fk": False, "uq": False},
            {"name": "current_balance", "type": "NUMERIC(15,2)", "pk": False, "fk": False, "uq": False},
            {"name": "movement_type", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "description", "type": "TEXT", "pk": False, "fk": False, "uq": False},
            {"name": "created_at", "type": "TIMESTAMPTZ", "pk": False, "fk": False, "uq": False},
        ]
    }
]

HEADER_H = 54
ROW_H = 26
FOOTER_PAD = 10

table_map = {}
for t in tables:
    t["height"] = HEADER_H + len(t["fields"]) * ROW_H + FOOTER_PAD
    table_map[t["id"]] = t

def get_field_center_y(t, field_name):
    for idx, f in enumerate(t["fields"]):
        if f["name"] == field_name:
            return t["y"] + HEADER_H + idx * ROW_H + (ROW_H // 2)
    return t["y"] + (t["height"] // 2)

# Color palettes
styles = {
    "catalog": {
        "header_grad": ("#581c87", "#7e22ce"),
        "accent": "#a855f7",
        "tag_bg": "rgba(168, 85, 247, 0.2)",
        "tag_text": "#d8b4fe",
        "border": "#7e22ce",
        "glow": "rgba(168, 85, 247, 0.25)"
    },
    "core": {
        "header_grad": ("#1e3a8a", "#2563eb"),
        "accent": "#3b82f6",
        "tag_bg": "rgba(59, 130, 246, 0.2)",
        "tag_text": "#93c5fd",
        "border": "#3b82f6",
        "glow": "rgba(59, 130, 246, 0.3)"
    },
    "details": {
        "header_grad": ("#064e3b", "#059669"),
        "accent": "#10b981",
        "tag_bg": "rgba(16, 185, 129, 0.2)",
        "tag_text": "#6ee7b7",
        "border": "#059669",
        "glow": "rgba(16, 185, 129, 0.25)"
    },
    "security": {
        "header_grad": ("#831843", "#db2777"),
        "accent": "#f43f5e",
        "tag_bg": "rgba(244, 63, 94, 0.2)",
        "tag_text": "#fda4af",
        "border": "#db2777",
        "glow": "rgba(244, 63, 94, 0.25)"
    },
    "finance": {
        "header_grad": ("#0c4a6e", "#0284c7"),
        "accent": "#0ea5e9",
        "tag_bg": "rgba(14, 165, 233, 0.2)",
        "tag_text": "#7dd3fc",
        "border": "#0284c7",
        "glow": "rgba(14, 165, 233, 0.25)"
    }
}

# Generate SVG
def generate_svg():
    svg_w = 1880
    svg_h = 1380
    
    parts = []
    parts.append(f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {svg_w} {svg_h}" width="{svg_w}" height="{svg_h}" style="background-color: #0b0f19; font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;">
    <defs>
        <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(255, 255, 255, 0.03)" stroke-width="1"/>
        </pattern>
        <linearGradient id="bg-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#0b1120"/>
            <stop offset="50%" stop-color="#0f172a"/>
            <stop offset="100%" stop-color="#020617"/>
        </linearGradient>
        <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
            <feDropShadow dx="0" dy="10" stdDeviation="12" flood-color="#000000" flood-opacity="0.6"/>
        </filter>
        <marker id="circle-end" markerWidth="8" markerHeight="8" refX="4" refY="4">
            <circle cx="4" cy="4" r="3" fill="#10b981"/>
        </marker>
        <marker id="crow-end" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 10 6 L 0 12 M 10 0 L 10 12" stroke="#38bdf8" stroke-width="2" fill="none"/>
        </marker>
        <marker id="crow-end-purple" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 10 6 L 0 12 M 10 0 L 10 12" stroke="#a855f7" stroke-width="2" fill="none"/>
        </marker>
        <marker id="crow-end-emerald" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 10 6 L 0 12 M 10 0 L 10 12" stroke="#10b981" stroke-width="2" fill="none"/>
        </marker>
        <marker id="crow-end-rose" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 10 6 L 0 12 M 10 0 L 10 12" stroke="#f43f5e" stroke-width="2" fill="none"/>
        </marker>
    </defs>

    <!-- Background -->
    <rect width="{svg_w}" height="{svg_h}" fill="url(#bg-grad)"/>
    <rect width="{svg_w}" height="{svg_h}" fill="url(#grid)"/>

    <!-- Header Banner -->
    <g transform="translate(60, 25)">
        <text x="0" y="24" fill="#f8fafc" font-size="24" font-weight="800" letter-spacing="0.5">MODELO ENTIDAD - RELACIÓN (BD BANCO)</text>
        <text x="560" y="24" fill="#94a3b8" font-size="14" font-weight="500">PostgreSQL Schema • Arquitectura Hexagonal • 12 Tablas • Clientes &amp; Finanzas</text>
        
        <!-- Category Legend Pills -->
        <g transform="translate(1160, 4)">
            <!-- Core -->
            <rect x="0" y="0" width="80" height="24" rx="12" fill="rgba(59, 130, 246, 0.2)" stroke="#3b82f6" stroke-width="1"/>
            <text x="40" y="16" fill="#93c5fd" font-size="11" font-weight="600" text-anchor="middle">Central</text>
            <!-- Catálogos -->
            <rect x="90" y="0" width="90" height="24" rx="12" fill="rgba(168, 85, 247, 0.2)" stroke="#a855f7" stroke-width="1"/>
            <text x="135" y="16" fill="#d8b4fe" font-size="11" font-weight="600" text-anchor="middle">Catálogos</text>
            <!-- Satélites -->
            <rect x="190" y="0" width="90" height="24" rx="12" fill="rgba(16, 185, 129, 0.2)" stroke="#10b981" stroke-width="1"/>
            <text x="235" y="16" fill="#6ee7b7" font-size="11" font-weight="600" text-anchor="middle">Datos 1:1</text>
            <!-- Seguridad -->
            <rect x="290" y="0" width="90" height="24" rx="12" fill="rgba(244, 63, 94, 0.2)" stroke="#f43f5e" stroke-width="1"/>
            <text x="335" y="16" fill="#fda4af" font-size="11" font-weight="600" text-anchor="middle">Seguridad</text>
            <!-- Finanzas -->
            <rect x="390" y="0" width="90" height="24" rx="12" fill="rgba(14, 165, 233, 0.2)" stroke="#0ea5e9" stroke-width="1"/>
            <text x="435" y="16" fill="#7dd3fc" font-size="11" font-weight="600" text-anchor="middle">Finanzas</text>
        </g>
    </g>
    ''')

    # Draw Connector Lines with independent non-overlapping channels
    state_bottom = table_map['state']['y'] + table_map['state']['height']
    connectors = [
        # 1. state -> municipality (vertical from bottom of state to top of muni)
        {
            "d": f"M 215, {state_bottom} L 215, {table_map['municipality']['y']}",
            "stroke": "#a855f7",
            "marker": "url(#crow-end-purple)",
            "label": "1 : N",
            "lx": 225, "ly": (state_bottom + table_map['municipality']['y']) // 2
        },
        # 2. municipality -> home (right of muni channel X=420 into home)
        {
            "d": f"M 370, {get_field_center_y(table_map['municipality'], 'id_municipality')} H 420 V {get_field_center_y(table_map['home'], 'id_municipality')} H 470",
            "stroke": "#10b981",
            "marker": "url(#crow-end-emerald)",
            "label": "1 : N",
            "lx": 420, "ly": 255
        },
        # 3. gender -> client (channel X=425)
        {
            "d": f"M 370, {get_field_center_y(table_map['gender'], 'id_gender')} H 430 V {get_field_center_y(table_map['client'], 'id_gender')} H 470",
            "stroke": "#a855f7",
            "marker": "url(#crow-end-purple)",
            "label": "1 : N",
            "lx": 430, "ly": 660
        },
        # 4. nationality -> client (channel X=410)
        {
            "d": f"M 370, {get_field_center_y(table_map['nationality'], 'id_nationality')} H 410 V {get_field_center_y(table_map['client'], 'id_nationality')} H 470",
            "stroke": "#a855f7",
            "marker": "url(#crow-end-purple)",
            "label": "1 : N",
            "lx": 410, "ly": 810
        },
        # 5. marital_status -> client (channel X=390)
        {
            "d": f"M 370, {get_field_center_y(table_map['marital_status'], 'id_marital_status')} H 390 V {get_field_center_y(table_map['client'], 'id_marital_status')} H 470",
            "stroke": "#a855f7",
            "marker": "url(#crow-end-purple)",
            "label": "1 : N",
            "lx": 390, "ly": 940
        },
        # 6. client -> home (vertical 1:1)
        {
            "d": f"M 650, {table_map['client']['y']} L 650, {table_map['home']['y'] + table_map['home']['height']}",
            "stroke": "#10b981",
            "marker": "url(#circle-end)",
            "label": "1 : 1",
            "lx": 660, "ly": 460
        },
        # 7. client -> contact_details (vertical 1:1)
        {
            "d": f"M 650, {table_map['client']['y'] + table_map['client']['height']} L 650, {table_map['contact_details']['y']}",
            "stroke": "#10b981",
            "marker": "url(#circle-end)",
            "label": "1 : 1",
            "lx": 660, "ly": 1000
        },
        # 8. client -> employment_information (1:1, channel X=880)
        {
            "d": f"M 830, {get_field_center_y(table_map['client'], 'id_client')} H 880 V {get_field_center_y(table_map['employment_information'], 'id_client')} H 950",
            "stroke": "#10b981",
            "marker": "url(#circle-end)",
            "label": "1 : 1",
            "lx": 880, "ly": 200
        },
        # 9. client -> auth (1:N, channel X=915)
        {
            "d": f"M 830, {get_field_center_y(table_map['client'], 'id_client') + 40} H 915 V {get_field_center_y(table_map['auth'], 'id_client')} H 950",
            "stroke": "#f43f5e",
            "marker": "url(#crow-end-rose)",
            "label": "1 : N",
            "lx": 915, "ly": 440
        },
        # 10. client -> account (1:N, channel X=890)
        {
            "d": f"M 830, {get_field_center_y(table_map['client'], 'id_client') + 80} H 890 V {get_field_center_y(table_map['account'], 'id_client')} H 950",
            "stroke": "#0ea5e9",
            "marker": "url(#crow-end)",
            "label": "1 : N",
            "lx": 890, "ly": 800
        },
        # 11. account -> account_balance (1:N, direct horizontal)
        {
            "d": f"M 1320, {get_field_center_y(table_map['account'], 'id_account')} H 1420",
            "stroke": "#0ea5e9",
            "marker": "url(#crow-end)",
            "label": "1 : N",
            "lx": 1370, "ly": get_field_center_y(table_map['account'], 'id_account') - 12
        }
    ]

    parts.append('<g id="relations">')
    for c in connectors:
        parts.append(f'''
            <path d="{c['d']}" fill="none" stroke="{c['stroke']}" stroke-width="2.5" stroke-dasharray="0" marker-end="{c['marker']}" opacity="0.9"/>
            <rect x="{c['lx'] - 18}" y="{c['ly'] - 10}" width="36" height="18" rx="4" fill="#0f172a" stroke="{c['stroke']}" stroke-width="1"/>
            <text x="{c['lx']}" y="{c['ly'] + 3}" fill="#e2e8f0" font-size="10" font-weight="700" text-anchor="middle">{c['label']}</text>
        ''')
    parts.append('</g>')

    # Draw Tables
    for t in tables:
        st = styles[t["category"]]
        x, y, w, h = t["x"], t["y"], t["width"], t["height"]
        
        # Table container with filter shadow
        parts.append(f'''
        <g id="table-{t['id']}" transform="translate({x}, {y})" filter="url(#shadow)">
            <!-- Outer Box -->
            <rect width="{w}" height="{h}" rx="10" fill="#111827" stroke="{st['border']}" stroke-width="1.5"/>
            
            <!-- Header Background with Gradient -->
            <defs>
                <linearGradient id="hdr-grad-{t['id']}" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="{st['header_grad'][0]}"/>
                    <stop offset="100%" stop-color="{st['header_grad'][1]}"/>
                </linearGradient>
            </defs>
            <path d="M 0 10 Q 0 0 10 0 L {w-10} 0 Q {w} 0 {w} 10 L {w} {HEADER_H} L 0 {HEADER_H} Z" fill="url(#hdr-grad-{t['id']})"/>
            
            <!-- Header Text -->
            <text x="14" y="24" fill="#ffffff" font-size="15" font-weight="800" letter-spacing="0.3">{t['name']}</text>
            <text x="14" y="42" fill="#cbd5e1" font-size="11" font-weight="500">{t['desc']}</text>
            
            <!-- Category Tag Pill in Header -->
            <rect x="{w - 110}" y="12" width="98" height="20" rx="10" fill="{st['tag_bg']}" stroke="{st['accent']}" stroke-width="0.8"/>
            <text x="{w - 61}" y="26" fill="{st['tag_text']}" font-size="9" font-weight="700" text-anchor="middle">{t['category_title']}</text>
            
            <!-- Table Header Bottom Divider -->
            <line x1="0" y1="{HEADER_H}" x2="{w}" y2="{HEADER_H}" stroke="rgba(255,255,255,0.15)" stroke-width="1"/>
            
            <!-- Columns Rows -->
            <g transform="translate(0, {HEADER_H})">
        ''')
        
        for i, f in enumerate(t["fields"]):
            ry = i * ROW_H
            row_bg = "rgba(255,255,255,0.02)" if i % 2 == 1 else "transparent"
            
            # Row background
            parts.append(f'<rect y="{ry}" width="{w}" height="{ROW_H}" fill="{row_bg}"/>')
            
            # Key Badges (PK, FK, UQ)
            badge_x = 12
            if f.get("pk"):
                parts.append(f'''
                    <rect x="{badge_x}" y="{ry + 4}" width="26" height="18" rx="4" fill="#f59e0b" />
                    <text x="{badge_x + 13}" y="{ry + 16}" fill="#0f172a" font-size="9" font-weight="900" text-anchor="middle">PK</text>
                ''')
                badge_x += 30
            if f.get("fk"):
                parts.append(f'''
                    <rect x="{badge_x}" y="{ry + 4}" width="26" height="18" rx="4" fill="#0ea5e9" />
                    <text x="{badge_x + 13}" y="{ry + 16}" fill="#0f172a" font-size="9" font-weight="900" text-anchor="middle">FK</text>
                ''')
                badge_x += 30
            if f.get("uq") and not f.get("pk"):
                parts.append(f'''
                    <rect x="{badge_x}" y="{ry + 4}" width="26" height="18" rx="4" fill="#6366f1" />
                    <text x="{badge_x + 13}" y="{ry + 16}" fill="#ffffff" font-size="9" font-weight="800" text-anchor="middle">UQ</text>
                ''')
                badge_x += 30

            # Column Name
            name_color = "#f8fafc" if f.get("pk") or f.get("fk") else "#e2e8f0"
            font_weight = "700" if f.get("pk") or f.get("fk") else "500"
            parts.append(f'<text x="{max(badge_x + 6, 46)}" y="{ry + 17}" fill="{name_color}" font-size="12" font-weight="{font_weight}">{f["name"]}</text>')
            
            # Data Type
            parts.append(f'<text x="{w - 14}" y="{ry + 17}" fill="#94a3b8" font-size="11" font-weight="500" font-family="Consolas, monospace" text-anchor="end">{f["type"]}</text>')

            # Row divider
            if i < len(t["fields"]) - 1:
                parts.append(f'<line x1="12" y1="{ry + ROW_H}" x2="{w - 12}" y2="{ry + ROW_H}" stroke="rgba(255,255,255,0.04)" stroke-width="1"/>')

        parts.append('''
            </g>
        </g>
        ''')

    parts.append('</svg>')
    return "".join(parts)


# Generate HTML with viewer, pan/zoom, and download buttons
def generate_html(svg_content):
    return f'''<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Modelo Entidad-Relación - Base de Datos Banco API</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        body {{
            background: #030712;
            color: #f8fafc;
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            overflow: hidden;
            height: 100vh;
            display: flex;
            flex-direction: column;
        }}
        /* Top Navigation Bar */
        header {{
            background: rgba(15, 23, 42, 0.85);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            padding: 12px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            z-index: 100;
        }}
        .brand {{
            display: flex;
            align-items: center;
            gap: 12px;
        }}
        .logo-badge {{
            background: linear-gradient(135deg, #2563eb, #3b82f6);
            width: 36px;
            height: 36px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 900;
            font-size: 18px;
            color: #fff;
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
        }}
        .title-group h1 {{
            font-size: 16px;
            font-weight: 700;
            letter-spacing: 0.3px;
        }}
        .title-group p {{
            font-size: 12px;
            color: #94a3b8;
        }}
        .controls {{
            display: flex;
            align-items: center;
            gap: 10px;
        }}
        .btn {{
            background: rgba(30, 41, 59, 0.8);
            color: #f1f5f9;
            border: 1px solid rgba(255, 255, 255, 0.15);
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            text-decoration: none;
        }}
        .btn:hover {{
            background: rgba(51, 65, 85, 0.9);
            border-color: rgba(255, 255, 255, 0.3);
            transform: translateY(-1px);
        }}
        .btn-primary {{
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            border: 1px solid #3b82f6;
            color: #ffffff;
            box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
        }}
        .btn-primary:hover {{
            background: linear-gradient(135deg, #3b82f6, #2563eb);
            box-shadow: 0 6px 20px rgba(37, 99, 235, 0.5);
        }}
        .btn-emerald {{
            background: linear-gradient(135deg, #059669, #047857);
            border: 1px solid #10b981;
            color: #ffffff;
            box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35);
        }}
        .btn-emerald:hover {{
            background: linear-gradient(135deg, #10b981, #059669);
            box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
        }}
        /* Canvas Workspace */
        #viewport {{
            flex: 1;
            position: relative;
            overflow: hidden;
            cursor: grab;
            background: radial-gradient(circle at 50% 50%, #0f172a 0%, #020617 100%);
        }}
        #viewport:active {{
            cursor: grabbing;
        }}
        #svg-wrapper {{
            position: absolute;
            top: 0;
            left: 0;
            transform-origin: 0 0;
            transition: transform 0.05s ease-out;
        }}
        /* Floating HUD */
        .hud {{
            position: absolute;
            bottom: 24px;
            right: 24px;
            display: flex;
            gap: 8px;
            background: rgba(15, 23, 42, 0.85);
            backdrop-filter: blur(8px);
            padding: 6px;
            border-radius: 12px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
            z-index: 50;
        }}
        .hud-btn {{
            width: 38px;
            height: 38px;
            border-radius: 8px;
            background: transparent;
            border: none;
            color: #cbd5e1;
            font-size: 18px;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.15s;
        }}
        .hud-btn:hover {{
            background: rgba(255, 255, 255, 0.1);
            color: #fff;
        }}
        .hud-indicator {{
            display: flex;
            align-items: center;
            padding: 0 10px;
            font-size: 12px;
            font-weight: 600;
            color: #94a3b8;
            font-family: monospace;
        }}
    </style>
</head>
<body>

    <header>
        <div class="brand">
            <div class="logo-badge">BD</div>
            <div class="title-group">
                <h1>Diagrama Entidad-Relación • Banco API</h1>
                <p>12 Tablas • Clientes, Catálogos INEGI, Cuentas, Domicilios y Auth</p>
            </div>
        </div>
        <div class="controls">
            <button class="btn" onclick="resetView()">Centrar Vista</button>
            <button class="btn btn-emerald" onclick="downloadSVG()">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Descargar SVG
            </button>
            <button class="btn btn-primary" onclick="downloadPNG()">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Descargar Imagen (PNG)
            </button>
        </div>
    </header>

    <div id="viewport">
        <div id="svg-wrapper">
            {svg_content}
        </div>
        
        <div class="hud">
            <button class="hud-btn" onclick="zoomIn()" title="Acercar">+</button>
            <div class="hud-indicator" id="zoom-indicator">100%</div>
            <button class="hud-btn" onclick="zoomOut()" title="Alejar">-</button>
            <button class="hud-btn" onclick="resetView()" title="Restablecer">⟲</button>
        </div>
    </div>

    <script>
        const viewport = document.getElementById('viewport');
        const wrapper = document.getElementById('svg-wrapper');
        const zoomIndicator = document.getElementById('zoom-indicator');
        const svgEl = wrapper.querySelector('svg');

        let scale = 0.85;
        let panX = 40;
        let panY = 20;
        let isDragging = false;
        let startX, startY;

        function updateTransform() {{
            wrapper.style.transform = `translate(${{panX}}px, ${{panY}}px) scale(${{scale}})`;
            zoomIndicator.textContent = Math.round(scale * 100) + '%';
        }}

        function zoomIn() {{
            scale = Math.min(scale * 1.2, 3.0);
            updateTransform();
        }}

        function zoomOut() {{
            scale = Math.max(scale / 1.2, 0.3);
            updateTransform();
        }}

        function resetView() {{
            const vw = viewport.clientWidth;
            const vh = viewport.clientHeight;
            const sw = 1880;
            const sh = 1380;
            scale = Math.min((vw - 80) / sw, (vh - 80) / sh, 1.0);
            panX = (vw - sw * scale) / 2;
            panY = (vh - sh * scale) / 2;
            updateTransform();
        }}

        viewport.addEventListener('mousedown', (e) => {{
            if (e.target.closest('.hud')) return;
            isDragging = true;
            startX = e.clientX - panX;
            startY = e.clientY - panY;
        }});

        window.addEventListener('mousemove', (e) => {{
            if (!isDragging) return;
            panX = e.clientX - startX;
            panY = e.clientY - startY;
            updateTransform();
        }});

        window.addEventListener('mouseup', () => {{
            isDragging = false;
        }});

        viewport.addEventListener('wheel', (e) => {{
            e.preventDefault();
            const delta = e.deltaY > 0 ? 0.9 : 1.1;
            const newScale = Math.min(Math.max(scale * delta, 0.3), 3.0);
            
            // Zoom towards mouse
            const rect = viewport.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const mouseY = e.clientY - rect.top;
            
            panX = mouseX - (mouseX - panX) * (newScale / scale);
            panY = mouseY - (mouseY - panY) * (newScale / scale);
            scale = newScale;
            updateTransform();
        }}, {{ passive: false }});

        // Initialize fit
        window.addEventListener('DOMContentLoaded', resetView);
        window.addEventListener('resize', resetView);

        // Download SVG
        function downloadSVG() {{
            const svgData = new XMLSerializer().serializeToString(svgEl);
            const blob = new Blob([svgData], {{ type: 'image/svg+xml;charset=utf-8' }});
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'modelo_entidad_relacion_banco.svg';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        }}

        // Download PNG
        function downloadPNG() {{
            const svgData = new XMLSerializer().serializeToString(svgEl);
            const svgBlob = new Blob([svgData], {{ type: 'image/svg+xml;charset=utf-8' }});
            const url = URL.createObjectURL(svgBlob);
            const img = new Image();
            
            img.onload = function() {{
                const canvas = document.createElement('canvas');
                const scaleDPI = 2; // High-resolution export
                canvas.width = 1880 * scaleDPI;
                canvas.height = 1380 * scaleDPI;
                const ctx = canvas.getContext('2d');
                ctx.scale(scaleDPI, scaleDPI);
                ctx.drawImage(img, 0, 0);
                
                const pngUrl = canvas.toDataURL('image/png');
                const link = document.createElement('a');
                link.href = pngUrl;
                link.download = 'modelo_entidad_relacion_banco.png';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                URL.revokeObjectURL(url);
            }};
            img.src = url;
        }}
    </script>
</body>
</html>
'''

def main():
    print("Generando SVG...")
    svg_content = generate_svg()
    
    workspace_dir = r"c:\Users\brand\Documentos\GIDS6102\Desarrollo web progresivo\Unidad 1\demo"
    svg_path = os.path.join(workspace_dir, "modelo_entidad_relacion.svg")
    html_path = os.path.join(workspace_dir, "modelo_entidad_relacion.html")
    png_path = os.path.join(workspace_dir, "modelo_entidad_relacion.png")

    with open(svg_path, "w", encoding="utf-8") as f:
        f.write(svg_content)
    print(f"SVG guardado en: {svg_path}")

    html_content = generate_html(svg_content)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"HTML guardado en: {html_path}")

    # Render PNG using Chrome or Edge Headless
    edge_bin = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
    chrome_bin = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
    browser = edge_bin if os.path.exists(edge_bin) else chrome_bin

    if os.path.exists(browser):
        print(f"Renderizando PNG de alta resolución usando {browser}...")
        cmd = [
            browser,
            "--headless=new",
            "--disable-gpu",
            "--force-device-scale-factor=1.5",
            f"--screenshot={png_path}",
            "--window-size=2820,2070",
            f"file:///{svg_path.replace(os.sep, '/')}"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        print("Salida navegador:", res.returncode)
        if os.path.exists(png_path):
            try:
                from PIL import Image
                img = Image.open(png_path)
                cropped = img.crop((0, 0, 2820, 2070))
                cropped.save(png_path, optimize=True)
                print(f"PNG recortado a dimensiones exactas 2820x2070: {png_path} ({os.path.getsize(png_path)} bytes)")
            except Exception as e:
                print("Error recortando:", e)
    
    # Also copy to artifact directory if available
    artifact_dir = r"C:\Users\brand\.gemini\antigravity-ide\brain\8e1724fe-b189-4bea-be66-3429362c1c8f"
    if os.path.exists(artifact_dir):
        if os.path.exists(png_path):
            shutil.copy(png_path, os.path.join(artifact_dir, "modelo_entidad_relacion.png"))
            print("Copiado PNG a directorio de artefactos.")
        shutil.copy(svg_path, os.path.join(artifact_dir, "modelo_entidad_relacion.svg"))
        print("Copiado SVG a directorio de artefactos.")

if __name__ == "__main__":
    main()

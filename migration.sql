-- SQL Migration Script for PostgreSQL (Render DB)
-- This creates the schema compatible with InfoAnimal SR models.

-- Enable uuid-ossp extension in case we want native UUIDs (optional)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create 'users' table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) DEFAULT '',
    zona_principal VARCHAR(255) DEFAULT '',
    rol VARCHAR(50) DEFAULT 'user',
    bloqueado BOOLEAN DEFAULT FALSE,
    foto_perfil VARCHAR(255) DEFAULT '',
    foto_portada VARCHAR(255) DEFAULT '',
    biografia TEXT DEFAULT '',
    created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create 'posts' table
CREATE TABLE IF NOT EXISTS posts (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL, -- e.g., 'perdida', 'encontrada', 'avistamiento', 'en_transito'
    especie VARCHAR(50) NOT NULL, -- e.g., 'perro', 'gato', 'otros'
    tamano VARCHAR(50) NOT NULL, -- e.g., 'pequeño', 'mediano', 'grande'
    nombre_mascota VARCHAR(255) DEFAULT '',
    descripcion TEXT NOT NULL,
    rasgos TEXT DEFAULT '',
    estado VARCHAR(50) DEFAULT 'activo',
    fotos JSONB DEFAULT '[]'::jsonb, -- Store list of photo filenames
    ubicacion_lat DOUBLE PRECISION DEFAULT 0.0,
    ubicacion_lng DOUBLE PRECISION DEFAULT 0.0,
    distrito VARCHAR(255) NOT NULL,
    zona VARCHAR(255) DEFAULT '',
    telefono_contacto VARCHAR(255) DEFAULT '',
    telefono_visible BOOLEAN DEFAULT TRUE,
    likes_count INTEGER DEFAULT 0,
    created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create 'comments' table
CREATE TABLE IF NOT EXISTS comments (
    id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    texto TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create 'likes' table (user bookmarks/likes for posts)
CREATE TABLE IF NOT EXISTS likes (
    id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_post_like UNIQUE (user_id, post_id)
);

-- 5. Create 'veterinarias' table
CREATE TABLE IF NOT EXISTS veterinarias (
    id VARCHAR(255) PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) DEFAULT '',
    latitud DOUBLE PRECISION DEFAULT 0.0,
    longitud DOUBLE PRECISION DEFAULT 0.0,
    created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_post_id ON likes(post_id);

-- Enable Row Level Security on posts table
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

-- Permitir lectura pública de publicaciones (usuarios anónimos y autenticados)
DROP POLICY IF EXISTS "Lectura publica de publicaciones" ON posts;
CREATE POLICY "Lectura publica de publicaciones" 
ON posts FOR SELECT 
USING (true);

-- Permitir al autor actualizar su propia publicación
DROP POLICY IF EXISTS "Los autores pueden actualizar sus publicaciones" ON posts;
CREATE POLICY "Los autores pueden actualizar sus publicaciones" 
ON posts FOR UPDATE TO authenticated 
USING (auth.uid() = user_id);

-- Permitir al autor eliminar su propia publicación
DROP POLICY IF EXISTS "Los autores pueden eliminar sus publicaciones" ON posts;
CREATE POLICY "Los autores pueden eliminar sus publicaciones" 
ON posts FOR DELETE TO authenticated 
USING (auth.uid() = user_id);

-- Enable Row Level Security on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Permitir lectura pública de usuarios
DROP POLICY IF EXISTS "Lectura publica de usuarios" ON users;
CREATE POLICY "Lectura publica de usuarios" 
ON users FOR SELECT 
USING (true);



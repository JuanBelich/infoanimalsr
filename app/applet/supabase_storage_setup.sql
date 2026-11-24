-- =========================================================================
-- Configuración de Supabase Storage y Políticas RLS para Fotos de Perfil
-- =========================================================================

-- Asegura la existencia de los buckets de almacenamiento como públicos ('profiles' y 'avatars')
INSERT INTO storage.buckets (id, name, public) 
VALUES ('profiles', 'profiles', true)
ON CONFLICT (id) DO UPDATE SET public = true;

INSERT INTO storage.buckets (id, name, public) 
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Políticas RLS para lectura pública
DROP POLICY IF EXISTS "Acceso publico lectura" ON storage.objects;
CREATE POLICY "Acceso publico lectura" 
ON storage.objects FOR SELECT 
USING (bucket_id = 'profiles' OR bucket_id = 'avatars');

-- Políticas RLS para permitir subir a usuarios autenticados
DROP POLICY IF EXISTS "Permitir subida a autenticados" ON storage.objects;
CREATE POLICY "Permitir subida a autenticados" 
ON storage.objects FOR INSERT 
TO authenticated 
WITH CHECK (bucket_id = 'profiles' OR bucket_id = 'avatars');

-- Políticas RLS para permitir actualización a usuarios autenticados
DROP POLICY IF EXISTS "Permitir actualizacion a autenticados" ON storage.objects;
CREATE POLICY "Permitir actualizacion a autenticados" 
ON storage.objects FOR UPDATE 
TO authenticated 
USING (bucket_id = 'profiles' OR bucket_id = 'avatars');


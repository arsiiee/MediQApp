-- File: supabase/migrations/20261112110000_add_fcm_token_to_profiles.sql
-- Purpose: Add fcm_token column to profiles for Firebase Cloud Messaging

ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS fcm_token TEXT;

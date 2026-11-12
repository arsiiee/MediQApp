import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY')!
const supabaseServiceKey = Deno.env.get('SERVICE_ROLE_KEY')

// Minimal logging - only critical errors. Supabase dashboard logs every invocation
// regardless; reduce log volume via Dashboard → Settings → General → Logging level.

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, OPTIONS',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
      },
    })
  }

  try {
    // Verify the caller using their JWT (passed via Authorization header)
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: req.headers.get('Authorization')! } },
    })
    const { data: { user }, error: userError } = await userClient.auth.getUser()
    if (userError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), {
        status: 401,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      })
    }

    // Check caller is ADMIN by reading their profile
    const { data: profile } = await userClient
      .from('profiles')
      .select('role')
      .eq('user_id', user.id)
      .single()
    if (profile?.role !== 'ADMIN') {
      return new Response(JSON.stringify({ error: 'Admin only' }), {
        status: 403,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      })
    }

    // Parse request body
    const body = await req.json()
    const {
      email,
      username,
      fullName,
      password,
      role,
      specializationId,
      licenseNumber,
      assignedDoctorId,
      mobileNumber,
      dateOfBirth,
    } = body

    if (!email || !username || !fullName || !role || !password) {
      return new Response(JSON.stringify({ error: 'Missing required fields' }), {
        status: 400,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      })
    }

    const usernameClean = String(username).trim()
    const emailClean = String(email).trim().toLowerCase()
    const roleUpper = String(role).toUpperCase()

    // Create auth user via service-role client (bypasses RLS)
    if (!supabaseServiceKey) {
      return new Response(JSON.stringify({ error: 'Server misconfigured: SERVICE_ROLE_KEY not set in Edge Function environment' }), {
        status: 500,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      })
    }
    const adminClient = createClient(supabaseUrl, supabaseServiceKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    })

    // Preflight duplicate checks for clearer errors (users/profiles have unique username)
    const { data: existingUsername, error: usernameCheckErr } = await adminClient
      .from('users')
      .select('id, auth_user_id, email')
      .ilike('username', usernameClean)
      .maybeSingle()
    if (usernameCheckErr) throw usernameCheckErr
    const { data: existingEmailRow, error: emailCheckErr } = await adminClient
      .from('users')
      .select('id, auth_user_id')
      .ilike('email', emailClean)
      .maybeSingle()
    if (emailCheckErr) throw emailCheckErr

    let userId: string
    // Check if email already exists in Supabase Auth
    const { data: listData, error: listErr } = await adminClient.auth.admin.listUsers()
    if (listErr) throw listErr
    const existingUsers = (listData as any)?.users || (listData as any) || []
    const existingAuth = existingUsers.find((u: any) => String(u.email || '').toLowerCase() === emailClean)

    if (existingUsername && (!existingAuth || String(existingUsername.auth_user_id || existingUsername.id) !== String(existingAuth.id))) {
      return new Response(JSON.stringify({ error: 'Username is already taken.' }), {
        status: 409,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      })
    }
    if (existingEmailRow && (!existingAuth || String(existingEmailRow.auth_user_id || existingEmailRow.id) !== String(existingAuth.id))) {
      return new Response(JSON.stringify({ error: 'Email is already registered.' }), {
        status: 409,
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
      })
    }

    if (existingAuth) {
      userId = existingAuth.id
      const { error: updateErr } = await adminClient.auth.admin.updateUserById(userId, { password, email_confirm: true })
      if (updateErr) throw updateErr
    } else {
      const { data: createData, error: createErr } = await adminClient.auth.admin.createUser({
        email: emailClean,
        password,
        email_confirm: true,
      })
      if (createErr) throw createErr
      if (!createData?.user?.id) throw new Error('Auth user creation returned no user id')
      userId = createData.user.id
    }

    const now = new Date().toISOString()

    // Insert into users table (Ktor domain model) — upsert
    const { error: usersErr } = await adminClient.from('users').upsert({
      id: userId,
      auth_user_id: userId,
      username: usernameClean,
      full_name: String(fullName).trim(),
      email: emailClean,
      mobile_number: mobileNumber || '',
      date_of_birth: dateOfBirth || '1970-01-01',
      role: roleUpper,
      status: 'ACTIVE',
      doctor_id: null,
      assigned_doctor_id: assignedDoctorId || null,
      created_at: now,
      updated_at: now,
    }, { onConflict: 'id' })
    if (usersErr) throw usersErr

    // Insert into profiles table (upsert in case of auto-trigger)
    const { error: profilesErr } = await adminClient.from('profiles').upsert({
      user_id: userId,
      username: usernameClean,
      full_name: String(fullName).trim(),
      email: emailClean,
      mobile_number: mobileNumber || null,
      role: roleUpper,
      status: 'ACTIVE',
      assigned_doctor_id: assignedDoctorId || null,
    }, { onConflict: 'user_id' })
    if (profilesErr) throw profilesErr

    // Insert into doctors table if doctor — upsert
    if (roleUpper === 'DOCTOR') {
      if (!specializationId) {
        throw new Error('specializationId is required for doctor accounts')
      }
      const { error: doctorsErr } = await adminClient.from('doctors').upsert({
        id: userId,
        user_id: userId,
        full_name: String(fullName).trim(),
        specialty: '',
        specialization_id: specializationId,
        license_number: licenseNumber || '',
        years_of_experience: 0,
        fee_centavos: 0,
        bio: '',
        building: '',
        floor: '',
        room: '',
        languages: '',
        status: 'ACTIVE',
        created_at: now,
        updated_at: now,
      }, { onConflict: 'id' })
      if (doctorsErr) throw doctorsErr
    }

    return new Response(
      JSON.stringify({ success: true, userId, role: roleUpper }),
      {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
        },
      },
    )
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
      },
    })
  }
})

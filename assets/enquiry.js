const ENDPOINT='https://anmrzqcvzxhjvmitkczc.supabase.co/functions/v1/regent-venture-enquiries';
const form=document.querySelector('#venture-enquiry-form');
if(form){
 const started=Date.now();const startedField=form.querySelector('[name="form_started_at"]');if(startedField)startedField.value=String(started);
 const params=new URLSearchParams(location.search);if(params.get('service')==='assessment'){const box=form.querySelector('input[value="Strategy"]');if(box)box.checked=true;const text=form.querySelector('[name="enquiry_text"]');if(text&&!text.value)text.value='I would like to request a Regent Venture Assessment for a venture or commercial opportunity.';}
 const status=document.querySelector('#form-status');const success=document.querySelector('#form-success');const submit=form.querySelector('button[type="submit"]');
 form.addEventListener('submit',async event=>{
  event.preventDefault();status.className='form-status';status.textContent='Submitting securely…';submit.disabled=true;
  const fd=new FormData(form);const services=[...form.querySelectorAll('input[name="requested_services"]:checked')].map(el=>el.value);
  const payload={action:'submit',full_name:fd.get('full_name'),email:fd.get('email'),phone:fd.get('phone'),company_name:fd.get('company_name'),website:fd.get('website'),enquiry_type:fd.get('enquiry_type'),venture_stage:fd.get('venture_stage'),requested_services:services,enquiry_text:fd.get('enquiry_text'),budget:fd.get('budget'),timeframe:fd.get('timeframe'),consent:fd.get('consent')==='yes',company_fax:fd.get('company_fax'),form_started_at:Number(fd.get('form_started_at')),source_url:location.href};
  try{const r=await fetch(ENDPOINT,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});const data=await r.json().catch(()=>({}));if(!r.ok)throw new Error(data.error||'We could not submit the enquiry.');form.reset();if(startedField)startedField.value=String(Date.now());success.classList.add('show');success.querySelector('[data-reference]').textContent=data.reference||'Recorded';status.textContent='';success.scrollIntoView({behavior:'smooth',block:'center'});}catch(error){status.className='form-status error';status.textContent=error.message||'We could not submit the enquiry. Please try again.';}finally{submit.disabled=false;}
 });
}

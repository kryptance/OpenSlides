#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
    openslides.projector.views
    ~~~~~~~~~~~~~~~~~~~~~~~

    Views for the projector app.

    :copyright: 2011 by the OpenSlides team, see AUTHORS.
    :license: GNU GPL, see LICENSE for more details.
"""
from datetime import datetime
from time import time

from django.shortcuts import render_to_response, redirect
from django.template import RequestContext
from django.core.urlresolvers import reverse
from django.utils.translation import ugettext as _
from django.utils.datastructures import SortedDict
from django.dispatch import receiver
from django.template.loader import render_to_string
from django.db.models import Q


from utils.views import TemplateView, RedirectView
from utils.utils import template, permission_required, \
                                   del_confirm_form, ajax_request
from utils.template import render_block_to_string
from utils.template import Tab

from config.models import config

from api import get_active_slide, set_active_slide, projector_message_set
from projector import SLIDE
from models import ProjectorOverlay
from openslides.projector.signals import projector_overlays, projector_control_box
from openslides.utils.signals import template_manipulation

from django.utils.importlib import import_module
import settings


class ControlView(TemplateView):
    template_name = 'projector/control.html'
    permission_required = 'projector.can_manage_projector'

    def get_projector_overlays(self):
        overlays = []
        for receiver, name in projector_overlays.send(sender='registerer', register=True):
            if name is not None:
                try:
                    projector_overlay = ProjectorOverlay.objects.get(def_name=name)
                except ProjectorOverlay.DoesNotExist:
                    projector_overlay = ProjectorOverlay(def_name=name, active=False)
                    projector_overlay.save()
                overlays.append(projector_overlay)
        return overlays

    def post(self, request, *args, **kwargs):
        if 'message' in request.POST:
            projector_message_set(request.POST['message_text'])
        else:
            for overlay in self.get_projector_overlays():
                if overlay.def_name in request.POST:
                    overlay.active = True
                else:
                    overlay.active = False
                overlay.save()
        return self.get(request, *args, **kwargs)

    def get_context_data(self, **kwargs):
        context = super(ControlView, self).get_context_data(**kwargs)
        categories = {}
        for slide in SLIDE.values():
            if not categories.has_key(slide.category):
                categories[slide.category] = []
            categories[slide.category].append(slide)

        tmp_categories = categories
        categories = SortedDict()
        for app in settings.INSTALLED_APPS:
            if app in tmp_categories:
                tmp_categories[app].sort(key=lambda slide: slide.weight)
                categories[app] = tmp_categories[app]


        ## for receiver, response in projector_control_box.send(sender='ControllView'):
            ## if response is not None:
                ## categories[response[0]] = response[1]

        context.update({
            'categories': categories,
            'countdown_visible': config['countdown_visible'],
            'countdown_time': config['agenda_countdown_time'],
            'overlays': self.get_projector_overlays(),
        })
        return context


class ActivateView(RedirectView):
    url = 'projector_control'
    allow_ajax = True

    def pre_redirect(self, request, *args, **kwargs):
        set_active_slide(kwargs['sid'])

    def get_ajax_context(self, **kwargs):
        context = super(ActivateView, self).get_ajax_context()
        return context


@permission_required('projector.can_see_projector')
def active_slide(request):
    """
    Shows the active Slide.
    """
    try:
        data = get_active_slide()
    except AttributeError: #TODO: It has to be an Slide.DoesNotExist
        data = None

    if data is None:
        data = {
            'title': config['event_name'],
            'template': 'projector/default.html',
        }

    data['ajax'] = 'on'
    data['overlays'] = []
    data['overlay'] = ''

    # Projector Overlays
    sid = get_active_slide(True)
    active_defs = ProjectorOverlay.objects.filter(active=True).filter(Q(sid=sid) | Q(sid=None)).values_list('def_name', flat=True)
    for receiver, response in projector_overlays.send(sender=sid, register=False, call=active_defs):
        if response is not None:
            data['overlays'].append(response)


    template_manipulation.send(sender='projector', request=request, context=data)
    if request.is_ajax():
        content = render_block_to_string(data['template'], 'content', data)
        jsondata = {
            'content': content,
            'overlays': data['overlays'],
            'title': data['title'],
            'time': datetime.now().strftime('%H:%M'),
            'bigger': config['bigger'],
            'up': config['up'],
            'countdown_visible': config['countdown_visible'],
            'countdown_time': config['agenda_countdown_time'],
            'countdown_control': config['countdown_control'],
            'overlay': data['overlay']
        }
        return ajax_request(jsondata)
    else:
        return render_to_response(
            data['template'],
            data,
            context_instance=RequestContext(request)
        )


@permission_required('agenda.can_manage_agenda')
def projector_edit(request, direction):
    if direction == 'bigger':
        config['bigger'] = int(config['bigger']) + 10
    elif direction == 'smaller':
        config['bigger'] = int(config['bigger']) - 10
    elif direction == 'up':
        config['up'] = int(config['up']) - 10
    elif direction == 'down':
        config['up'] = int(config['up']) + 10
    elif direction == 'clean':
        config['up'] = 0
        config['bigger'] = 100

    if request.is_ajax():
        return ajax_request({})
    return redirect(reverse('projector_control'))


@permission_required('projector.can_manage_projector')
def projector_countdown(request, command):
    #todo: why is there the time argument?
    if command == 'show':
        config['countdown_visible'] = True
    elif command == 'hide':
        config['countdown_visible'] = False
    elif command == 'reset':
        config['countdown_start'] = time()
    elif command == 'start':
        config['countdown_run'] = True
    elif command == 'stop':
        config['countdown_run'] = False

    if request.is_ajax():
        if command == "show":
            link = reverse('countdown_close')
        else:
            link = reverse('countdown_open')
        return ajax_request({'countdown_visible': config['countdown_visible'],
                             'link': link})
    return redirect(reverse('projector_control'))


def register_tab(request):
    selected = True if request.path.startswith('/projector/') else False
    return Tab(
        title=_('Projector'),
        url=reverse('projector_control'),
        permission=request.user.has_perm('projector.can_manag_projector'),
        selected=selected,
    )


## @receiver(projector_control_box, dispatch_uid="openslides.projector.views.projector_box")
## def projector_box(sender, **kwargs):
    ## return ('header', 'text')
